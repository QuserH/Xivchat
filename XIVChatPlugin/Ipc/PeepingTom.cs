using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Linq;
using Dalamud.Game.ClientState.Actors.Types;
using Lumina.Excel.GeneratedSheets;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Client;
using XIVChatCommon.Message.Server;

namespace XIVChatPlugin.Ipc {
    internal class PeepingTom : IDisposable {
        private Plugin Plugin { get; }

        internal PeepingTom(Plugin plugin) {
            this.Plugin = plugin;

            #pragma warning disable 618
            this.Plugin.Interface.Subscribe("PeepingTom", this.ReceiveTargeting);
            #pragma warning restore 618
        }

        public void Dispose() {
            #pragma warning disable 618
            this.Plugin.Interface.Unsubscribe("PeepingTom");
            #pragma warning restore 618
        }

        private void ReceiveTargeting(dynamic obj) {
            List<ExpandoObject> list = obj.Targeting;
            var players = list
                .Select((dynamic player) => (uint) player.ActorId)
                .Select(targeting => this.Plugin.Interface.ClientState.Actors.FirstOrDefault(actor => actor.ActorId == targeting))
                .Where(actor => actor is PlayerCharacter)
                .Cast<PlayerCharacter>()
                .Select(chara => new Player {
                    Name = chara.Name,
                    FreeCompany = chara.CompanyTag,
                    Status = 0,
                    CurrentWorld = (ushort) chara.CurrentWorld.Id,
                    CurrentWorldName = chara.CurrentWorld.GameData.Name.ToString(),
                    HomeWorld = (ushort) chara.HomeWorld.Id,
                    HomeWorldName = chara.HomeWorld.GameData.Name.ToString(),
                    Territory = this.Plugin.Interface.ClientState.TerritoryType,
                    TerritoryName = this.Plugin.Interface.Data.GetExcelSheet<TerritoryType>().GetRow(this.Plugin.Interface.ClientState.TerritoryType)?.Name,
                    Job = (byte) chara.ClassJob.Id,
                    JobName = chara.ClassJob.GameData.Name.ToString(),
                    GrandCompany = 0,
                    GrandCompanyName = null,
                    Languages = 0,
                    MainLanguage = 0,
                })
                .ToArray();

            foreach (var client in this.Plugin.Server.Clients.Values) {
                if (!client.GetPreference(ClientPreference.TargetingListSupport, false)) {
                    continue;
                }

                client.Queue.Writer.TryWrite(new ServerPlayerList(PlayerListType.Targeting, players));
            }
        }
    }
}
