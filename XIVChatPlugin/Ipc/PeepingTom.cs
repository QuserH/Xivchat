using System;
using System.Collections.Generic;
using System.Linq;
using Dalamud.Game.ClientState.Objects.SubKinds;
using Lumina.Excel.GeneratedSheets;
using PeepingTom.Ipc;
using PeepingTom.Ipc.From;
using PeepingTom.Ipc.To;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Client;
using XIVChatCommon.Message.Server;

namespace XIVChatPlugin.Ipc {
    internal class PeepingTom : IDisposable {
        private Plugin Plugin { get; }
        private List<TargeterWithStatus> Targeters { get; } = new();

        private class TargeterWithStatus {
            public Targeter Targeter { get; set; } = null!;
            public bool Targeting { get; set; }
        }

        internal PeepingTom(Plugin plugin) {
            this.Plugin = plugin;

            IpcInfo.GetSubscriber(this.Plugin.Interface).Subscribe(this.ReceiveMessage);
            IpcInfo.GetProvider(this.Plugin.Interface).SendMessage(new RequestTargetersMessage());
            this.Plugin.Events.NewClient += this.OnNewClient;
        }

        public void Dispose() {
            this.Plugin.Events.NewClient -= this.OnNewClient;
            IpcInfo.GetSubscriber(this.Plugin.Interface).Unsubscribe(this.ReceiveMessage);
        }

        private void ReceiveMessage(IFromMessage message) {
            switch (message) {
                case AllTargetersMessage allMessage: {
                    this.Targeters.Clear();
                    this.Targeters.AddRange(allMessage.Targeters
                        .Select(t => new TargeterWithStatus {
                            Targeter = t.targeter,
                            Targeting = t.currentlyTargeting,
                        }));
                    break;
                }
                case NewTargeterMessage newMessage: {
                    this.UpdateTargeter(newMessage.Targeter, true);

                    break;
                }
                case StoppedTargetingMessage stoppedMessage: {
                    this.UpdateTargeter(stoppedMessage.Targeter, false);

                    break;
                }
            }

            var xivChatMessage = this.GetMessage();
            foreach (var client in this.Plugin.Server.Clients.Values) {
                this.SendToClient(client, xivChatMessage);
            }
        }

        private void UpdateTargeter(Targeter targeter, bool targeting) {
            var existing = this.Targeters.FirstOrDefault(t => t.Targeter.ObjectId == targeter.ObjectId);
            if (existing == default) {
                this.Targeters.Add(new TargeterWithStatus {
                    Targeter = targeter,
                    Targeting = targeting,
                });
            } else {
                existing.Targeter = targeter;
                existing.Targeting = targeting;
            }
        }

        private ServerPlayerList GetMessage() {
            var players = this.Targeters
                .Where(targeter => targeter.Targeting) // FIXME: send entire history so clients don't have to do logic
                .Select(targeter => this.Plugin.ObjectTable.FirstOrDefault(obj => obj.ObjectId == targeter.Targeter.ObjectId))
                .Where(actor => actor is PlayerCharacter)
                .Cast<PlayerCharacter>()
                .Select(chara => new Player {
                    Name = chara.Name.TextValue,
                    FreeCompany = chara.CompanyTag.TextValue,
                    Status = 0,
                    CurrentWorld = (ushort) chara.CurrentWorld.Id,
                    CurrentWorldName = chara.CurrentWorld.GameData.Name.ToString(),
                    HomeWorld = (ushort) chara.HomeWorld.Id,
                    HomeWorldName = chara.HomeWorld.GameData.Name.ToString(),
                    Territory = this.Plugin.ClientState.TerritoryType,
                    TerritoryName = this.Plugin.DataManager.GetExcelSheet<TerritoryType>()!.GetRow(this.Plugin.ClientState.TerritoryType)?.Name?.ToString(),
                    Job = (byte) chara.ClassJob.Id,
                    JobName = chara.ClassJob.GameData.Name.ToString(),
                    GrandCompany = 0,
                    GrandCompanyName = null,
                    Languages = 0,
                    MainLanguage = 0,
                })
                .ToArray();
            return new ServerPlayerList(PlayerListType.Targeting, players);
        }

        private void SendToClient(BaseClient client, ServerPlayerList list) {
            if (!client.GetPreference(ClientPreference.TargetingListSupport, false)) {
                return;
            }

            client.Queue.Writer.TryWrite(list);
        }

        private void OnNewClient(Guid id, BaseClient client) {
            this.SendToClient(client, this.GetMessage());
        }
    }
}
