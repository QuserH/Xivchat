.class public final Lio/annaclemens/xivchat/model/message/ServerInventorySummary;
.super Ljava/lang/Object;
.source "ServerInventorySummary.kt"

# instance fields
.field private final itemCount:I
.field private final totalQuantity:I

# direct methods
.method public constructor <init>(II)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput p1, p0, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;->itemCount:I
    iput p2, p0, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;->totalQuantity:I
    return-void
.end method

.method public static final read(Ljava/nio/ByteBuffer;)Lio/annaclemens/xivchat/model/message/ServerInventorySummary;
    .locals 10
    invoke-static {p0}, Lorg/msgpack/core/MessagePack;->newDefaultUnpacker(Ljava/nio/ByteBuffer;)Lorg/msgpack/core/MessageUnpacker;
    move-result-object v0
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackArrayHeader()I
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackLong()J
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackArrayHeader()I
    move-result v1
    const/4 v2, 0x0
    const/4 v3, 0x0
    :loop
    if-ge v2, v1, :done
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackLong()J
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackLong()J
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackLong()J
    move-result-wide v4
    long-to-int v4, v4
    add-int/2addr v3, v4
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackLong()J
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackLong()J
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackBoolean()Z
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackLong()J
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackLong()J
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->tryUnpackNil()Z
    move-result v4
    if-nez v4, :skip_name
    invoke-virtual {v0}, Lorg/msgpack/core/MessageUnpacker;->unpackString()Ljava/lang/String;
    :skip_name
    add-int/lit8 v2, v2, 0x1
    goto :loop
    :done
    new-instance v0, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;
    invoke-direct {v0, v1, v3}, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;-><init>(II)V
    return-object v0
.end method

# virtual methods
.method public final getItemCount()I
    .locals 1
    iget v0, p0, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;->itemCount:I
    return v0
.end method

.method public final getTotalQuantity()I
    .locals 1
    iget v0, p0, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;->totalQuantity:I
    return v0
.end method

