.class public final enum Lio/annaclemens/xivchat/model/ServerOperation;
.super Ljava/lang/Enum;
.source "Operations.kt"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lio/annaclemens/xivchat/model/ServerOperation$Companion;
    }
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lio/annaclemens/xivchat/model/ServerOperation;",
        ">;"
    }
.end annotation

.annotation system Ldalvik/annotation/SourceDebugExtension;
    value = "SMAP\nOperations.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Operations.kt\nio/annaclemens/xivchat/model/ServerOperation\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n*L\n1#1,36:1\n8774#2,2:37\n9034#2,4:39\n*E\n*S KotlinDebug\n*F\n+ 1 Operations.kt\nio/annaclemens/xivchat/model/ServerOperation\n*L\n15#1,2:37\n15#1,4:39\n*E\n"
.end annotation

.annotation runtime Lkotlin/Metadata;
    bv = {
        0x1,
        0x0,
        0x3
    }
    d1 = {
        "\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u0005\n\u0002\u0008\u000e\u0008\u0086\u0001\u0018\u0000 \u00102\u0008\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\u0010B\u000f\u0008\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0008\n\u0000\u001a\u0004\u0008\u0005\u0010\u0006j\u0002\u0008\u0007j\u0002\u0008\u0008j\u0002\u0008\tj\u0002\u0008\nj\u0002\u0008\u000bj\u0002\u0008\u000cj\u0002\u0008\rj\u0002\u0008\u000ej\u0002\u0008\u000f\u00a8\u0006\u0011"
    }
    d2 = {
        "Lio/annaclemens/xivchat/model/ServerOperation;",
        "",
        "code",
        "",
        "(Ljava/lang/String;IB)V",
        "getCode",
        "()B",
        "Pong",
        "Message",
        "Shutdown",
        "PlayerData",
        "Availability",
        "Channel",
        "Backlog",
        "PlayerList",
        "LinkshellList",
        "Companion",
        "app_release"
    }
    k = 0x1
    mv = {
        0x1,
        0x4,
        0x2
    }
.end annotation


# static fields
.field private static final synthetic $VALUES:[Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum Availability:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum Backlog:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum Channel:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final Companion:Lio/annaclemens/xivchat/model/ServerOperation$Companion;

.field public static final enum LinkshellList:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum Inventory:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum Message:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum PlayerData:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum PlayerList:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum Pong:Lio/annaclemens/xivchat/model/ServerOperation;

.field public static final enum Shutdown:Lio/annaclemens/xivchat/model/ServerOperation;

.field private static final map:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/Byte;",
            "Lio/annaclemens/xivchat/model/ServerOperation;",
            ">;"
        }
    .end annotation
.end field


# instance fields
.field private final code:B


# direct methods
.method static constructor <clinit>()V
    .locals 7

    const/16 v0, 0xa

    new-array v1, v0, [Lio/annaclemens/xivchat/model/ServerOperation;

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "Pong"

    const/4 v4, 0x0

    const/4 v5, 0x1

    .line 4
    invoke-direct {v2, v3, v4, v5}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->Pong:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v4

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "Message"

    const/4 v6, 0x2

    .line 5
    invoke-direct {v2, v3, v5, v6}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->Message:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v5

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "Shutdown"

    const/4 v5, 0x3

    .line 6
    invoke-direct {v2, v3, v6, v5}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->Shutdown:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v6

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "PlayerData"

    const/4 v6, 0x4

    .line 7
    invoke-direct {v2, v3, v5, v6}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->PlayerData:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v5

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "Availability"

    const/4 v5, 0x5

    .line 8
    invoke-direct {v2, v3, v6, v5}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->Availability:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v6

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "Channel"

    const/4 v6, 0x6

    .line 9
    invoke-direct {v2, v3, v5, v6}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->Channel:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v5

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "Backlog"

    const/4 v5, 0x7

    .line 10
    invoke-direct {v2, v3, v6, v5}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->Backlog:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v6

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "PlayerList"

    const/16 v6, 0x8

    .line 11
    invoke-direct {v2, v3, v5, v6}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->PlayerList:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v5

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "LinkshellList"

    const/16 v0, 0x9

    .line 12
    invoke-direct {v2, v3, v6, v0}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->LinkshellList:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v6

    new-instance v2, Lio/annaclemens/xivchat/model/ServerOperation;

    const-string v3, "Inventory"

    const/16 v4, 0x9

    const/16 v5, 0xb

    invoke-direct {v2, v3, v4, v5}, Lio/annaclemens/xivchat/model/ServerOperation;-><init>(Ljava/lang/String;IB)V

    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->Inventory:Lio/annaclemens/xivchat/model/ServerOperation;

    aput-object v2, v1, v4

    sput-object v1, Lio/annaclemens/xivchat/model/ServerOperation;->$VALUES:[Lio/annaclemens/xivchat/model/ServerOperation;

    new-instance v0, Lio/annaclemens/xivchat/model/ServerOperation$Companion;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Lio/annaclemens/xivchat/model/ServerOperation$Companion;-><init>(Lkotlin/jvm/internal/DefaultConstructorMarker;)V

    sput-object v0, Lio/annaclemens/xivchat/model/ServerOperation;->Companion:Lio/annaclemens/xivchat/model/ServerOperation$Companion;

    .line 15
    invoke-static {}, Lio/annaclemens/xivchat/model/ServerOperation;->values()[Lio/annaclemens/xivchat/model/ServerOperation;

    move-result-object v0

    .line 37
    array-length v1, v0

    invoke-static {v1}, Lkotlin/collections/MapsKt;->mapCapacity(I)I

    move-result v1

    const/16 v2, 0x10

    invoke-static {v1, v2}, Lkotlin/ranges/RangesKt;->coerceAtLeast(II)I

    move-result v1

    .line 38
    new-instance v2, Ljava/util/LinkedHashMap;

    invoke-direct {v2, v1}, Ljava/util/LinkedHashMap;-><init>(I)V

    check-cast v2, Ljava/util/Map;

    .line 39
    array-length v1, v0

    const/4 v4, 0x0

    :goto_0
    if-ge v4, v1, :cond_0

    aget-object v3, v0, v4

    .line 15
    iget-byte v5, v3, Lio/annaclemens/xivchat/model/ServerOperation;->code:B

    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    move-result-object v5

    invoke-interface {v2, v5, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    add-int/lit8 v4, v4, 0x1

    goto :goto_0

    .line 38
    :cond_0
    sput-object v2, Lio/annaclemens/xivchat/model/ServerOperation;->map:Ljava/util/Map;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;IB)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(B)V"
        }
    .end annotation

    .line 3
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    iput-byte p3, p0, Lio/annaclemens/xivchat/model/ServerOperation;->code:B

    return-void
.end method

.method public static final synthetic access$getMap$cp()Ljava/util/Map;
    .locals 1

    .line 3
    sget-object v0, Lio/annaclemens/xivchat/model/ServerOperation;->map:Ljava/util/Map;

    return-object v0
.end method

.method public static valueOf(Ljava/lang/String;)Lio/annaclemens/xivchat/model/ServerOperation;
    .locals 1

    const-class v0, Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lio/annaclemens/xivchat/model/ServerOperation;

    return-object p0
.end method

.method public static values()[Lio/annaclemens/xivchat/model/ServerOperation;
    .locals 1

    sget-object v0, Lio/annaclemens/xivchat/model/ServerOperation;->$VALUES:[Lio/annaclemens/xivchat/model/ServerOperation;

    invoke-virtual {v0}, [Lio/annaclemens/xivchat/model/ServerOperation;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lio/annaclemens/xivchat/model/ServerOperation;

    return-object v0
.end method


# virtual methods
.method public final getCode()B
    .locals 1

    .line 3
    iget-byte v0, p0, Lio/annaclemens/xivchat/model/ServerOperation;->code:B

    return v0
.end method

