using System;
using System.Collections.Generic;
using System.Reflection;
using MessagePack;

namespace XIVChatCommon.Message.Client {
    [MessagePackObject]
    public class ClientPreferences : Encodable {
        [Key(0)]
        public Dictionary<ClientPreference, object> Preferences { get; set; } = new();

        protected override byte Code => (byte) ClientOperation.Preferences;

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }

        public static ClientPreferences Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientPreferences>(bytes);
        }
    }

    public enum ClientPreference {
        [Preference(typeof(bool))]
        BacklogNewestMessagesFirst = 0,

        [Preference(typeof(bool))]
        TargetingListSupport = 1,
        
        [Preference(typeof(bool))]
        HousingLocationSupport = 2,

        [Preference(typeof(bool))]
        PhoneInventorySupport = 3,

        [Preference(typeof(bool))]
        PhoneWalletSupport = 4,

        [Preference(typeof(bool))]
        PhoneWeatherSupport = 5,

        [Preference(typeof(bool))]
        PhoneJobsSupport = 6,

        [Preference(typeof(bool))]
        PhoneDailiesSupport = 7,

        [Preference(typeof(bool))]
        PhoneActivitySupport = 8,

        [Preference(typeof(bool))]
        PhoneCollectionsSupport = 9,

        [Preference(typeof(bool))]
        PhoneMapsSupport = 10,

        [Preference(typeof(bool))]
        PhoneFishingSupport = 11,
    }

    public static class ClientPreferencesExtension {
        public static bool TryGetValue<T>(this ClientPreferences prefs, ClientPreference pref, out T value) {
            value = default!;

            if (!prefs.Preferences.TryGetValue(pref, out var obj)) {
                return false;
            }

            var attr = pref
                .GetType()
                .GetField(pref.ToString())
                ?.GetCustomAttribute<PreferenceAttribute>(false);

            if (obj.GetType() != typeof(T) || obj.GetType() != attr?.ValueType) {
                return false;
            }

            value = (T) obj;
            return true;
        }
    }

    public class PreferenceAttribute : Attribute {
        public Type ValueType { get; }

        public PreferenceAttribute(Type valueType) {
            this.ValueType = valueType;
        }
    }
}
