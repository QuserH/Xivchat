.class public final synthetic Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$WhenMappings;
.super Ljava/lang/Object;


# annotations
.annotation runtime Lkotlin/Metadata;
    bv = {
        0x1,
        0x0,
        0x3
    }
    k = 0x3
    mv = {
        0x1,
        0x4,
        0x2
    }
.end annotation


# static fields
.field public static final synthetic $EnumSwitchMapping$0:[I

.field public static final synthetic $EnumSwitchMapping$1:[I


# direct methods
.method static synthetic constructor <clinit>()V
    .locals 3

    invoke-static {}, Lio/annaclemens/xivchat/model/message/PlayerListType;->values()[Lio/annaclemens/xivchat/model/message/PlayerListType;

    move-result-object v0

    array-length v0, v0

    new-array v0, v0, [I

    sput-object v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$WhenMappings;->$EnumSwitchMapping$0:[I

    sget-object v1, Lio/annaclemens/xivchat/model/message/PlayerListType;->Friend:Lio/annaclemens/xivchat/model/message/PlayerListType;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/message/PlayerListType;->ordinal()I

    move-result v1

    const/4 v2, 0x1

    aput v2, v0, v1

    invoke-static {}, Lio/annaclemens/xivchat/model/ServerOperation;->values()[Lio/annaclemens/xivchat/model/ServerOperation;

    move-result-object v0

    array-length v0, v0

    new-array v0, v0, [I

    sput-object v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$WhenMappings;->$EnumSwitchMapping$1:[I

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->Message:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->Pong:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/4 v2, 0x2

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->Shutdown:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/4 v2, 0x3

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->Availability:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/4 v2, 0x4

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->PlayerData:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/4 v2, 0x5

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->Channel:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/4 v2, 0x6

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->Backlog:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/4 v2, 0x7

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->PlayerList:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/16 v2, 0x8

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->LinkshellList:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/16 v2, 0x9

    aput v2, v0, v1

    sget-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->Inventory:Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result v1

    const/16 v2, 0xa

    aput v2, v0, v1

    return-void
.end method

