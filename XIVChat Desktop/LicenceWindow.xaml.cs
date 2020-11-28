using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows;
using Windows.Web.Http;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

namespace XIVChat_Desktop {
    public partial class LicenceWindow {
        public App App => (App)Application.Current;

        private readonly bool startAfterSuccess;

        public LicenceWindow(Window? owner, bool startAfterSuccess) {
            this.Owner = owner;
            this.startAfterSuccess = startAfterSuccess;
            this.InitializeComponent();
            this.DataContext = this;

            var empty = string.IsNullOrWhiteSpace(this.App.Config.LicenceKey);
            if (empty) {
                this.CheckButton.IsEnabled = true;
                this.LicenceDescription.Text = "No licence key has been supplied. Please enter one.";
                this.SetLoading(false);
                return;
            }

            _ = Task.Run(async () => {
                await this.Check();
                this.Dispatch(() => this.SetLoading(false));
            });
        }

        private void SetLoading(bool loading) {
            this.Loading.Visibility = loading ? Visibility.Visible : Visibility.Collapsed;
            this.LicenceInfoPanel.Visibility = loading ? Visibility.Collapsed : Visibility.Visible;
        }

        private async Task<bool> Check(bool increment = false) {
            var key = this.App.Config.LicenceKey;
            if (string.IsNullOrWhiteSpace(key)) {
                return false;
            }

            this.Dispatch(() => this.CheckButton.IsEnabled = false);

            var licence = await LicenceInfo(key, increment);
            var valid = licence.Valid();

            this.Dispatch(() => {
                this.CheckButton.IsEnabled = true;
                this.LicenceDescription.Text = valid
                    ? $"Valid licence key for {licence.purchase.email}. Thank you!"
                    : "Invalid licence key. Please check the key and try again.";
            });

            return valid;
        }

        private async void Check_Click(object sender, RoutedEventArgs e) {
            this.App.Config.Save();

            var valid = await this.Check(true);

            if (!valid || !this.startAfterSuccess) {
                return;
            }

            this.App.InitialiseWindow();
            this.Close();
        }

        internal static async Task<LicenceResponse> LicenceInfo(string key, bool increment = false) {
            var uri = new Uri("https://api.gumroad.com/v2/licenses/verify");
            var data = new Dictionary<string, string> {
                ["product_permalink"] = "kvQIw",
                ["license_key"] = key,
                ["increment_uses_count"] = increment ? "true" : "false",
            };

            var client = new HttpClient();
            var res = await client.PostAsync(uri, new HttpFormUrlEncodedContent(data));
            var response = JsonConvert.DeserializeObject<LicenceResponse>(await res.Content.ReadAsStringAsync());

            return response;
        }
    }

    [JsonObject(NamingStrategyType = typeof(SnakeCaseNamingStrategy))]
    public class LicenceResponse {
        public readonly bool success;
        public readonly uint uses;
        public readonly Purchase purchase;

        public LicenceResponse(bool success, uint uses, Purchase purchase) {
            this.success = success;
            this.uses = uses;
            this.purchase = purchase;
        }

        public bool Valid() {
            return this.success && !this.purchase.refunded && !this.purchase.chargebacked;
        }
    }


    [JsonObject(NamingStrategyType = typeof(SnakeCaseNamingStrategy))]
    public class Purchase {
        public readonly string id;
        public readonly string productName;
        public readonly DateTime createdAt;
        public readonly string fullName;
        public readonly bool refunded;
        public readonly bool chargebacked;
        public readonly string email;

        public Purchase(string id, string productName, DateTime createdAt, string fullName, bool refunded, bool chargebacked, string email) {
            this.id = id;
            this.productName = productName;
            this.createdAt = createdAt;
            this.fullName = fullName;
            this.refunded = refunded;
            this.chargebacked = chargebacked;
            this.email = email;
        }
    }
}
