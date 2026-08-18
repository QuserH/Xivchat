.class public final Lio/annaclemens/xivchat/InventoryUiUpdate;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;
.source "InventoryUiUpdate.java"

.field private final activity:Lio/annaclemens/xivchat/MainActivity;
.field private final itemCount:I
.field private final totalQuantity:I

.method public constructor <init>(Lio/annaclemens/xivchat/MainActivity;II)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lio/annaclemens/xivchat/InventoryUiUpdate;->activity:Lio/annaclemens/xivchat/MainActivity;
    iput p2, p0, Lio/annaclemens/xivchat/InventoryUiUpdate;->itemCount:I
    iput p3, p0, Lio/annaclemens/xivchat/InventoryUiUpdate;->totalQuantity:I
    return-void
.end method

.method public final run()V
    .locals 3
    iget-object v0, p0, Lio/annaclemens/xivchat/InventoryUiUpdate;->activity:Lio/annaclemens/xivchat/MainActivity;
    iget v1, p0, Lio/annaclemens/xivchat/InventoryUiUpdate;->itemCount:I
    iget v2, p0, Lio/annaclemens/xivchat/InventoryUiUpdate;->totalQuantity:I
    invoke-virtual {v0, v1, v2}, Lio/annaclemens/xivchat/MainActivity;->updateInventorySummary(II)V
    return-void
.end method

