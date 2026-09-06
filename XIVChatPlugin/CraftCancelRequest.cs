namespace XIVChatPlugin;

/// <summary>Empty legacy requests only release preparation; confirmed cancellation is instance-scoped.</summary>
internal sealed record CraftCancelRequest(int RecipeId, long InstanceId) {
    public bool ReleaseOnly => RecipeId == 0;

    public static CraftCancelRequest? Decode(long[] values) => values.Length switch {
        0 => new(0, 0),
        2 when values[0] is > 0 and <= int.MaxValue && values[1] >= 0 => new((int)values[0], values[1]),
        _ => null,
    };

    public bool Matches(int recipeId, long instanceId, bool crafting) =>
        !ReleaseOnly && InstanceId > 0 && crafting && RecipeId == recipeId && InstanceId == instanceId;

    public static bool OwnsDialog(ushort synthesisId, ushort parentId, ushort hostId, ushort blockedParentId) =>
        synthesisId != 0 && (parentId == synthesisId || hostId == synthesisId || blockedParentId == synthesisId);
}
