using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerWeatherWindow {
        [Key(0)] public string Name { get; set; } = string.Empty;
        [Key(1)] public int MinutesFromNow { get; set; }
        [Key(2)] public int EorzeaBell { get; set; }
    }

    [MessagePackObject]
    public sealed class ServerWeather : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public string Zone { get; set; } = string.Empty;
        [Key(2)] public string Current { get; set; } = string.Empty;
        [Key(3)] public ServerWeatherWindow[] Forecast { get; set; } = [];

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Weather;

        public ServerWeather() {
        }

        public ServerWeather(long updatedUnix, string zone, string current, ServerWeatherWindow[] forecast) {
            UpdatedUnix = updatedUnix;
            Zone = zone;
            Current = current;
            Forecast = forecast;
        }

        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
