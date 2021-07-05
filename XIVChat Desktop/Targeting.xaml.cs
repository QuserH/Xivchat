using System.Windows;

namespace XIVChat_Desktop {
    public partial class Targeting {
        public App App => (App) Application.Current;

        public Targeting(Window owner) {
            this.Owner = owner;
            this.DataContext = this;

            this.InitializeComponent();
        }
    }
}
