.class public final Lio/annaclemens/xivchat/model/message/ClientBacklog;
.super Ljava/lang/Object;
.source "ClientMessage.kt"

.field private final amount:S

.method private constructor <init>(S)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-short p1, p0, Lio/annaclemens/xivchat/model/message/ClientBacklog;->amount:S
    return-void
.end method

.method public synthetic constructor <init>(SLkotlin/jvm/internal/DefaultConstructorMarker;)V
    .locals 0
    invoke-direct {p0, p1}, Lio/annaclemens/xivchat/model/message/ClientBacklog;-><init>(S)V
    return-void
.end method

.method public final encode()[B
    .locals 10
    invoke-static {}, Lorg/msgpack/core/MessagePack;->newDefaultBufferPacker()Lorg/msgpack/core/MessageBufferPacker;
    move-result-object v0

    const/4 v1, 0x2
    invoke-virtual {v0, v1}, Lorg/msgpack/core/MessageBufferPacker;->packArrayHeader(I)Lorg/msgpack/core/MessagePacker;

    iget-short v2, p0, Lio/annaclemens/xivchat/model/message/ClientBacklog;->amount:S
    invoke-virtual {v0, v2}, Lorg/msgpack/core/MessageBufferPacker;->packShort(S)Lorg/msgpack/core/MessagePacker;

    const v2, 0x4550
    invoke-virtual {v0, v2}, Lorg/msgpack/core/MessageBufferPacker;->packInt(I)Lorg/msgpack/core/MessagePacker;

    invoke-virtual {v0}, Lorg/msgpack/core/MessageBufferPacker;->close()V
    invoke-virtual {v0}, Lorg/msgpack/core/MessageBufferPacker;->toByteArray()[B
    move-result-object v3

    array-length v0, v3
    const/4 v1, 0x1
    add-int/2addr v0, v1
    new-array v0, v0, [B

    sget-object v1, Lio/annaclemens/xivchat/model/ClientOperation;->Backlog:Lio/annaclemens/xivchat/model/ClientOperation;
    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ClientOperation;->getCode()B
    move-result v1
    const/4 v2, 0x0
    aput-byte v1, v0, v2

    const-string v1, "bytes"
    invoke-static {v3, v1}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    const/4 v5, 0x1
    const/4 v6, 0x0
    const/4 v7, 0x0
    const/16 v8, 0xc
    const/4 v9, 0x0
    move-object v4, v0
    invoke-static/range {v3 .. v9}, Lkotlin/collections/ArraysKt;->copyInto$default([B[BIIIILjava/lang/Object;)[B
    return-object v0
.end method
