using XIVChatPlugin;

var passed = 0;
void Check(bool ok, string name) {
    if (!ok) throw new InvalidOperationException(name);
    passed++;
    Console.WriteLine($"PASS {name}");
}

Check(!CraftCancelRequest.Decode([])!.Matches(35836, 77, true), "legacy release never cancels an active craft");
Check(CraftCancelRequest.Decode([35836]) == null, "reject incomplete confirmation");
Check(CraftCancelRequest.Decode([35836, -1]) == null, "reject negative identity");
var request = CraftCancelRequest.Decode([35836, 77])!;
Check(request.Matches(35836, 77, true), "matching confirmed craft may be cancelled");
Check(!request.Matches(35836, 78, true), "same recipe next craft is protected");
Check(!request.Matches(35837, 77, true), "another recipe is protected");
Check(!request.Matches(35836, 77, false), "finished craft is protected");
Check(!CraftCancelRequest.Decode([35836, 0])!.Matches(35836, 77, true), "preparation cancellation never cancels live craft");
Check(CraftCancelRequest.OwnsDialog(42, 42, 0, 0), "own synthesis dialog is allowed");
Check(!CraftCancelRequest.OwnsDialog(42, 43, 44, 45), "unrelated dialog is protected");
Check(!CraftCancelRequest.OwnsDialog(0, 0, 0, 0), "empty dialog ownership is rejected");
Console.WriteLine($"{passed} crafting safety checks passed.");
