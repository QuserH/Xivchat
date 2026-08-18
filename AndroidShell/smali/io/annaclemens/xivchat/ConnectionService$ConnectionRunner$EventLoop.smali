.class final Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;
.super Ljava/lang/Object;
.source "ConnectionService.kt"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1a
    name = "EventLoop"
.end annotation

.annotation system Ldalvik/annotation/SourceDebugExtension;
    value = "SMAP\nConnectionService.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ConnectionService.kt\nio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop\n+ 2 Select.kt\nkotlinx/coroutines/selects/SelectKt\n+ 3 ActivityViewModelLazy.kt\nandroidx/activity/ActivityViewModelLazyKt\n*L\n1#1,734:1\n205#2,11:735\n41#3,7:746\n41#3,7:753\n41#3,7:760\n41#3,7:767\n*E\n*S KotlinDebug\n*F\n+ 1 ConnectionService.kt\nio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop\n*L\n526#1,11:735\n600#1,7:746\n607#1,7:753\n641#1,7:760\n650#1,7:767\n*E\n"
.end annotation

.annotation runtime Lkotlin/Metadata;
    bv = {
        0x1,
        0x0,
        0x3
    }
    d1 = {
        "\u0000J\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0012\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0003\n\u0002\u0010\u0005\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0005\u0008\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J7\u0010\u0007\u001a\u00020\u00082\u0006\u0010\t\u001a\u00020\n2\u000c\u0010\u000b\u001a\u0008\u0012\u0004\u0012\u00020\r0\u000c2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0012J!\u0010\u0013\u001a\u00020\u00082\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0082@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0018J=\u0010\u0019\u001a\u00020\u00082\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u000c\u0010\u000b\u001a\u0008\u0012\u0004\u0012\u00020\r0\u000c2\u000c\u0010\u001a\u001a\u0008\u0012\u0004\u0012\u00020\u00080\u000cH\u0082@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u001bR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\u0008\u0019\u00a8\u0006\u001c"
    }
    d2 = {
        "Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;",
        "",
        "runner",
        "Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;",
        "(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)V",
        "loop",
        "",
        "run",
        "",
        "conn",
        "Lio/ktor/network/sockets/Socket;",
        "messagesIn",
        "Lkotlinx/coroutines/channels/ReceiveChannel;",
        "",
        "txStream",
        "Lio/ktor/utils/io/ByteWriteChannel;",
        "tx",
        "Lcom/goterl/lazycode/lazysodium/utils/Key;",
        "(Lio/ktor/network/sockets/Socket;Lkotlinx/coroutines/channels/ReceiveChannel;Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;",
        "runServerOperation",
        "code",
        "",
        "payload",
        "Ljava/nio/ByteBuffer;",
        "(BLjava/nio/ByteBuffer;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;",
        "select",
        "pingTicker",
        "(Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;",
        "app_release"
    }
    k = 0x1
    mv = {
        0x1,
        0x4,
        0x2
    }
.end annotation


# instance fields
.field private loop:Z

.field private final runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;


# direct methods
.method public constructor <init>(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)V
    .locals 1

    const-string v0, "runner"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 507
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    const/4 p1, 0x1

    .line 508
    iput-boolean p1, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->loop:Z

    return-void
.end method

.method public static final synthetic access$getLoop$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;)Z
    .locals 0

    .line 507
    iget-boolean p0, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->loop:Z

    return p0
.end method

.method public static final synthetic access$getRunner$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;)Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;
    .locals 0

    .line 507
    iget-object p0, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    return-object p0
.end method

.method public static final synthetic access$setLoop$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Z)V
    .locals 0

    .line 507
    iput-boolean p1, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->loop:Z

    return-void
.end method


# virtual methods
.method public final run(Lio/ktor/network/sockets/Socket;Lkotlinx/coroutines/channels/ReceiveChannel;Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .locals 17
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lio/ktor/network/sockets/Socket;",
            "Lkotlinx/coroutines/channels/ReceiveChannel<",
            "[B>;",
            "Lio/ktor/utils/io/ByteWriteChannel;",
            "Lcom/goterl/lazycode/lazysodium/utils/Key;",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    move-object/from16 v0, p5

    instance-of v1, v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;

    if-eqz v1, :cond_0

    move-object v1, v0

    check-cast v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;

    iget v2, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->label:I

    const/high16 v3, -0x80000000

    and-int/2addr v2, v3

    if-eqz v2, :cond_0

    iget v0, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->label:I

    sub-int/2addr v0, v3

    iput v0, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->label:I

    move-object/from16 v2, p0

    goto :goto_0

    :cond_0
    new-instance v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;

    move-object/from16 v2, p0

    invoke-direct {v1, v2, v0}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;-><init>(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lkotlin/coroutines/Continuation;)V

    :goto_0
    iget-object v0, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->result:Ljava/lang/Object;

    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    move-result-object v3

    .line 511
    iget v4, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->label:I

    const/4 v5, 0x1

    if-eqz v4, :cond_2

    if-ne v4, v5, :cond_1

    iget-object v4, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$5:Ljava/lang/Object;

    check-cast v4, Lkotlinx/coroutines/channels/ReceiveChannel;

    iget-object v6, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$4:Ljava/lang/Object;

    check-cast v6, Lcom/goterl/lazycode/lazysodium/utils/Key;

    iget-object v7, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$3:Ljava/lang/Object;

    check-cast v7, Lio/ktor/utils/io/ByteWriteChannel;

    iget-object v8, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$2:Ljava/lang/Object;

    check-cast v8, Lkotlinx/coroutines/channels/ReceiveChannel;

    iget-object v9, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$1:Ljava/lang/Object;

    check-cast v9, Lio/ktor/network/sockets/Socket;

    iget-object v10, v1, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$0:Ljava/lang/Object;

    check-cast v10, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;

    :try_start_0
    invoke-static {v0}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto/16 :goto_4

    :catch_0
    move-exception v0

    goto/16 :goto_3

    .line 522
    :cond_1
    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v1, "call to \'resume\' before \'invoke\' with coroutine"

    invoke-direct {v0, v1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 511
    :cond_2
    invoke-static {v0}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    const-wide/16 v6, 0x7530

    const-wide/16 v8, 0x7530

    const/4 v10, 0x0

    const/4 v11, 0x0

    const/16 v12, 0xc

    const/4 v13, 0x0

    .line 513
    invoke-static/range {v6 .. v13}, Lkotlinx/coroutines/channels/TickerChannelsKt;->ticker$default(JJLkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/channels/TickerMode;ILjava/lang/Object;)Lkotlinx/coroutines/channels/ReceiveChannel;

    move-result-object v0

    move-object/from16 v4, p3

    move-object/from16 v12, p4

    move-object v15, v0

    move-object v13, v1

    move-object v11, v2

    move-object v14, v3

    move-object/from16 v1, p1

    move-object/from16 v3, p2

    .line 515
    :goto_1
    move-object v0, v1

    check-cast v0, Lio/ktor/network/sockets/ASocket;

    invoke-static {v0}, Lio/ktor/network/sockets/SocketsKt;->isClosed(Lio/ktor/network/sockets/ASocket;)Z

    move-result v0

    if-nez v0, :cond_4

    iget-boolean v0, v11, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->loop:Z

    if-eqz v0, :cond_4

    .line 517
    :try_start_1
    iput-object v11, v13, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$0:Ljava/lang/Object;

    iput-object v1, v13, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$1:Ljava/lang/Object;

    iput-object v3, v13, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$2:Ljava/lang/Object;

    iput-object v4, v13, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$3:Ljava/lang/Object;

    iput-object v12, v13, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$4:Ljava/lang/Object;

    iput-object v15, v13, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->L$5:Ljava/lang/Object;

    iput v5, v13, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$run$1;->label:I
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_2

    move-object v6, v11

    move-object v7, v4

    move-object v8, v12

    move-object v9, v3

    move-object v10, v15

    move-object/from16 v16, v11

    move-object v11, v13

    :try_start_2
    invoke-virtual/range {v6 .. v11}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->select(Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object v0
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_1

    if-ne v0, v14, :cond_3

    return-object v14

    :cond_3
    move-object v9, v1

    move-object v8, v3

    move-object v7, v4

    move-object v6, v12

    move-object v1, v13

    move-object v3, v14

    move-object v4, v15

    move-object/from16 v10, v16

    goto :goto_4

    :catch_1
    move-exception v0

    goto :goto_2

    :catch_2
    move-exception v0

    move-object/from16 v16, v11

    :goto_2
    move-object v9, v1

    move-object v8, v3

    move-object v7, v4

    move-object v6, v12

    move-object v1, v13

    move-object v3, v14

    move-object v4, v15

    move-object/from16 v10, v16

    .line 519
    :goto_3
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    :goto_4
    move-object v13, v1

    move-object v14, v3

    move-object v15, v4

    move-object v12, v6

    move-object v4, v7

    move-object v3, v8

    move-object v1, v9

    move-object v11, v10

    goto :goto_1

    .line 522
    :cond_4
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

.method final synthetic runServerOperation(BLjava/nio/ByteBuffer;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .locals 7
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(B",
            "Ljava/nio/ByteBuffer;",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    instance-of v0, p3, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;

    if-eqz v0, :cond_0

    move-object v0, p3

    check-cast v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;

    iget v1, v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;->label:I

    const/high16 v2, -0x80000000

    and-int/2addr v1, v2

    if-eqz v1, :cond_0

    iget p3, v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;->label:I

    sub-int/2addr p3, v2

    iput p3, v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;->label:I

    goto :goto_0

    :cond_0
    new-instance v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;

    invoke-direct {v0, p0, p3}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;-><init>(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lkotlin/coroutines/Continuation;)V

    :goto_0
    iget-object p3, v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;->result:Ljava/lang/Object;

    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    move-result-object v1

    .line 583
    iget v2, v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;->label:I

    const/4 v3, 0x1

    if-eqz v2, :cond_2

    if-ne v2, v3, :cond_1

    invoke-static {p3}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    goto/16 :goto_1

    .line 667
    :cond_1
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "call to \'resume\' before \'invoke\' with coroutine"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    .line 583
    :cond_2
    invoke-static {p3}, Lkotlin/ResultKt;->throwOnFailure(Ljava/lang/Object;)V

    .line 584
    sget-object p3, Lio/annaclemens/xivchat/model/ServerOperation;->Companion:Lio/annaclemens/xivchat/model/ServerOperation$Companion;

    int-to-byte p1, p1

    invoke-virtual {p3, p1}, Lio/annaclemens/xivchat/model/ServerOperation$Companion;->fromCode(B)Lio/annaclemens/xivchat/model/ServerOperation;

    move-result-object p1

    if-nez p1, :cond_3

    goto/16 :goto_1

    :cond_3
    sget-object p3, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$WhenMappings;->$EnumSwitchMapping$1:[I

    invoke-virtual {p1}, Lio/annaclemens/xivchat/model/ServerOperation;->ordinal()I

    move-result p1

    aget p1, p3, p1

    const/4 p3, 0x0

    packed-switch p1, :pswitch_data_0

    :pswitch_0
    goto/16 :goto_1

    .line 647
    :pswitch_1
    sget-object p1, Lio/annaclemens/xivchat/model/message/ServerPlayerList;->Companion:Lio/annaclemens/xivchat/model/message/ServerPlayerList$Companion;

    invoke-virtual {p1, p2}, Lio/annaclemens/xivchat/model/message/ServerPlayerList$Companion;->read(Ljava/nio/ByteBuffer;)Lio/annaclemens/xivchat/model/message/ServerPlayerList;

    move-result-object p1

    .line 648
    invoke-virtual {p1}, Lio/annaclemens/xivchat/model/message/ServerPlayerList;->getType()Lio/annaclemens/xivchat/model/message/PlayerListType;

    move-result-object p2

    if-nez p2, :cond_4

    goto/16 :goto_1

    :cond_4
    sget-object v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$WhenMappings;->$EnumSwitchMapping$0:[I

    invoke-virtual {p2}, Lio/annaclemens/xivchat/model/message/PlayerListType;->ordinal()I

    move-result p2

    aget p2, v0, p2

    if-eq p2, v3, :cond_5

    goto/16 :goto_1

    .line 650
    :cond_5
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-virtual {p2}, Lio/annaclemens/xivchat/ConnectionService;->getApp()Lio/annaclemens/xivchat/MainActivity;

    move-result-object p2

    check-cast p2, Landroidx/activity/ComponentActivity;

    .line 767
    move-object v0, p3

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .line 769
    new-instance v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$7;

    invoke-direct {v0, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$7;-><init>(Landroidx/activity/ComponentActivity;)V

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .line 773
    new-instance v1, Landroidx/lifecycle/ViewModelLazy;

    const-class v2, Lio/annaclemens/xivchat/ui/friends/FriendListViewModel;

    invoke-static {v2}, Lkotlin/jvm/internal/Reflection;->getOrCreateKotlinClass(Ljava/lang/Class;)Lkotlin/reflect/KClass;

    move-result-object v2

    new-instance v3, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$8;

    invoke-direct {v3, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$8;-><init>(Landroidx/activity/ComponentActivity;)V

    check-cast v3, Lkotlin/jvm/functions/Function0;

    invoke-direct {v1, v2, v3, v0}, Landroidx/lifecycle/ViewModelLazy;-><init>(Lkotlin/reflect/KClass;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    check-cast v1, Lkotlin/Lazy;

    .line 651
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService;->access$getHandler$p(Lio/annaclemens/xivchat/ConnectionService;)Landroid/os/Handler;

    move-result-object p2

    new-instance v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$9;

    invoke-direct {v0, v1, p3, p1}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$9;-><init>(Lkotlin/Lazy;Lkotlin/reflect/KProperty;Lio/annaclemens/xivchat/model/message/ServerPlayerList;)V

    check-cast v0, Ljava/lang/Runnable;

    invoke-virtual {p2, v0}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    goto/16 :goto_1

    .line 640
    :pswitch_2
    sget-object p1, Lio/annaclemens/xivchat/model/message/ServerBacklog;->Companion:Lio/annaclemens/xivchat/model/message/ServerBacklog$Companion;

    invoke-virtual {p1, p2}, Lio/annaclemens/xivchat/model/message/ServerBacklog$Companion;->read(Ljava/nio/ByteBuffer;)Lio/annaclemens/xivchat/model/message/ServerBacklog;

    move-result-object p1

    .line 641
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-virtual {p2}, Lio/annaclemens/xivchat/ConnectionService;->getApp()Lio/annaclemens/xivchat/MainActivity;

    move-result-object p2

    check-cast p2, Landroidx/activity/ComponentActivity;

    .line 760
    move-object v0, p3

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .line 762
    new-instance v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$5;

    invoke-direct {v0, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$5;-><init>(Landroidx/activity/ComponentActivity;)V

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .line 766
    new-instance v1, Landroidx/lifecycle/ViewModelLazy;

    const-class v2, Lio/annaclemens/xivchat/ui/messages/MessagesViewModel;

    invoke-static {v2}, Lkotlin/jvm/internal/Reflection;->getOrCreateKotlinClass(Ljava/lang/Class;)Lkotlin/reflect/KClass;

    move-result-object v2

    new-instance v3, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$6;

    invoke-direct {v3, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$6;-><init>(Landroidx/activity/ComponentActivity;)V

    check-cast v3, Lkotlin/jvm/functions/Function0;

    invoke-direct {v1, v2, v3, v0}, Landroidx/lifecycle/ViewModelLazy;-><init>(Lkotlin/reflect/KClass;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    check-cast v1, Lkotlin/Lazy;

    .line 642
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService;->access$getHandler$p(Lio/annaclemens/xivchat/ConnectionService;)Landroid/os/Handler;

    move-result-object p2

    new-instance v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$8;

    invoke-direct {v0, v1, p3, p1}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$8;-><init>(Lkotlin/Lazy;Lkotlin/reflect/KProperty;Lio/annaclemens/xivchat/model/message/ServerBacklog;)V

    check-cast v0, Ljava/lang/Runnable;

    invoke-virtual {p2, v0}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    goto/16 :goto_1

    .line 633
    :pswitch_3
    sget-object p1, Lio/annaclemens/xivchat/model/message/Channel;->Companion:Lio/annaclemens/xivchat/model/message/Channel$Companion;

    invoke-virtual {p1, p2}, Lio/annaclemens/xivchat/model/message/Channel$Companion;->read(Ljava/nio/ByteBuffer;)Lio/annaclemens/xivchat/model/message/Channel;

    move-result-object p1

    .line 634
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService;->access$getHandler$p(Lio/annaclemens/xivchat/ConnectionService;)Landroid/os/Handler;

    move-result-object p2

    new-instance p3, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$7;

    invoke-direct {p3, p0, p1}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$7;-><init>(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lio/annaclemens/xivchat/model/message/Channel;)V

    check-cast p3, Ljava/lang/Runnable;

    invoke-virtual {p2, p3}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    goto/16 :goto_1

    .line 605
    :pswitch_4
    sget-object p1, Lio/annaclemens/xivchat/model/message/PlayerData;->Companion:Lio/annaclemens/xivchat/model/message/PlayerData$Companion;

    invoke-virtual {p1, p2}, Lio/annaclemens/xivchat/model/message/PlayerData$Companion;->read(Ljava/nio/ByteBuffer;)Lio/annaclemens/xivchat/model/message/PlayerData;

    move-result-object p1

    .line 607
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-virtual {p2}, Lio/annaclemens/xivchat/ConnectionService;->getApp()Lio/annaclemens/xivchat/MainActivity;

    move-result-object p2

    check-cast p2, Landroidx/activity/ComponentActivity;

    .line 753
    move-object v2, p3

    check-cast v2, Lkotlin/jvm/functions/Function0;

    .line 755
    new-instance v2, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$3;

    invoke-direct {v2, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$3;-><init>(Landroidx/activity/ComponentActivity;)V

    check-cast v2, Lkotlin/jvm/functions/Function0;

    .line 759
    new-instance v4, Landroidx/lifecycle/ViewModelLazy;

    const-class v5, Lio/annaclemens/xivchat/ui/servers/ServersViewModel;

    invoke-static {v5}, Lkotlin/jvm/internal/Reflection;->getOrCreateKotlinClass(Ljava/lang/Class;)Lkotlin/reflect/KClass;

    move-result-object v5

    new-instance v6, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$4;

    invoke-direct {v6, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$4;-><init>(Landroidx/activity/ComponentActivity;)V

    check-cast v6, Lkotlin/jvm/functions/Function0;

    invoke-direct {v4, v5, v6, v2}, Landroidx/lifecycle/ViewModelLazy;-><init>(Lkotlin/reflect/KClass;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    check-cast v4, Lkotlin/Lazy;

    .line 608
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService;->access$getHandler$p(Lio/annaclemens/xivchat/ConnectionService;)Landroid/os/Handler;

    move-result-object p2

    new-instance v2, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$4;

    invoke-direct {v2, v4, p3, p1}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$4;-><init>(Lkotlin/Lazy;Lkotlin/reflect/KProperty;Lio/annaclemens/xivchat/model/message/PlayerData;)V

    check-cast v2, Ljava/lang/Runnable;

    invoke-virtual {p2, v2}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    if-nez p1, :cond_6

    .line 613
    iget-object p1, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    move-object p2, p3

    check-cast p2, Ljava/lang/String;

    invoke-static {p1, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$setLastPlayerPortrait$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;Ljava/lang/String;)V

    .line 614
    iget-object p1, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p1}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p1

    invoke-static {p1}, Lio/annaclemens/xivchat/ConnectionService;->access$getHandler$p(Lio/annaclemens/xivchat/ConnectionService;)Landroid/os/Handler;

    move-result-object p1

    new-instance p2, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$5;

    invoke-direct {p2, v4, p3}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$5;-><init>(Lkotlin/Lazy;Lkotlin/reflect/KProperty;)V

    check-cast p2, Ljava/lang/Runnable;

    invoke-virtual {p1, p2}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    .line 618
    sget-object p1, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object p1

    .line 622
    :cond_6
    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1}, Lio/annaclemens/xivchat/model/message/PlayerData;->getName()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {p2, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const/16 v2, 0x2f

    invoke-virtual {p2, v2}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Lio/annaclemens/xivchat/model/message/PlayerData;->getHomeWorld()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {p2, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    .line 623
    iget-object v2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {v2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getLastPlayerPortrait$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Ljava/lang/String;

    move-result-object v2

    invoke-static {p2, v2}, Lkotlin/jvm/internal/Intrinsics;->areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v2

    xor-int/2addr v2, v3

    if-eqz v2, :cond_7

    .line 625
    iget-object v2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    new-instance v4, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$6;

    invoke-direct {v4, p0, p2, p3}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$6;-><init>(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Ljava/lang/String;Lkotlin/coroutines/Continuation;)V

    check-cast v4, Lkotlin/jvm/functions/Function2;

    iput v3, v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$1;->label:I

    invoke-virtual {v2, p1, v4, v0}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->downloadPlayerPortrait(Lio/annaclemens/xivchat/model/message/PlayerData;Lkotlin/jvm/functions/Function2;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

    move-result-object p1

    if-ne p1, v1, :cond_7

    return-object v1

    .line 599
    :pswitch_5
    sget-object p1, Lio/annaclemens/xivchat/model/message/Availability;->Companion:Lio/annaclemens/xivchat/model/message/Availability$Companion;

    invoke-virtual {p1, p2}, Lio/annaclemens/xivchat/model/message/Availability$Companion;->read(Ljava/nio/ByteBuffer;)Lio/annaclemens/xivchat/model/message/Availability;

    move-result-object p1

    .line 600
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-virtual {p2}, Lio/annaclemens/xivchat/ConnectionService;->getApp()Lio/annaclemens/xivchat/MainActivity;

    move-result-object p2

    check-cast p2, Landroidx/activity/ComponentActivity;

    .line 746
    move-object v0, p3

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .line 748
    new-instance v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$1;

    invoke-direct {v0, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$1;-><init>(Landroidx/activity/ComponentActivity;)V

    check-cast v0, Lkotlin/jvm/functions/Function0;

    .line 752
    new-instance v1, Landroidx/lifecycle/ViewModelLazy;

    const-class v2, Lio/annaclemens/xivchat/ui/messages/MessagesViewModel;

    invoke-static {v2}, Lkotlin/jvm/internal/Reflection;->getOrCreateKotlinClass(Ljava/lang/Class;)Lkotlin/reflect/KClass;

    move-result-object v2

    new-instance v3, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$2;

    invoke-direct {v3, p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$$inlined$viewModels$2;-><init>(Landroidx/activity/ComponentActivity;)V

    check-cast v3, Lkotlin/jvm/functions/Function0;

    invoke-direct {v1, v2, v3, v0}, Landroidx/lifecycle/ViewModelLazy;-><init>(Lkotlin/reflect/KClass;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V

    check-cast v1, Lkotlin/Lazy;

    .line 601
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService;->access$getHandler$p(Lio/annaclemens/xivchat/ConnectionService;)Landroid/os/Handler;

    move-result-object p2

    new-instance v0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$3;

    invoke-direct {v0, v1, p3, p1}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$3;-><init>(Lkotlin/Lazy;Lkotlin/reflect/KProperty;Lio/annaclemens/xivchat/model/message/Availability;)V

    check-cast v0, Ljava/lang/Runnable;

    invoke-virtual {p2, v0}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    goto :goto_1

    :pswitch_6
    const/4 p1, 0x0

    .line 596
    iput-boolean p1, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->loop:Z

    goto :goto_1

    .line 587
    :pswitch_7
    sget-object p1, Lio/annaclemens/xivchat/model/message/ServerMessage;->Companion:Lio/annaclemens/xivchat/model/message/ServerMessage$Companion;

    invoke-virtual {p1, p2}, Lio/annaclemens/xivchat/model/message/ServerMessage$Companion;->read(Ljava/nio/ByteBuffer;)Lio/annaclemens/xivchat/model/message/ServerMessage;

    move-result-object p1

    .line 588
    iget-object p2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object p2

    invoke-static {p2}, Lio/annaclemens/xivchat/ConnectionService;->access$getHandler$p(Lio/annaclemens/xivchat/ConnectionService;)Landroid/os/Handler;

    move-result-object p2

    new-instance p3, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$2;

    invoke-direct {p3, p0, p1}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$runServerOperation$2;-><init>(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lio/annaclemens/xivchat/model/message/ServerMessage;)V

    check-cast p3, Ljava/lang/Runnable;

    invoke-virtual {p2, p3}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    .line 667
    :cond_7
    :goto_1
    sget-object p1, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object p1

    .line 670
    :pswitch_8
    invoke-static {p2}, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;->read(Ljava/nio/ByteBuffer;)Lio/annaclemens/xivchat/model/message/ServerInventorySummary;

    move-result-object p1

    iget-object v2, p0, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->runner:Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    invoke-static {v2}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object v2

    invoke-virtual {v2}, Lio/annaclemens/xivchat/ConnectionService;->getApp()Lio/annaclemens/xivchat/MainActivity;

    move-result-object v3

    invoke-virtual {p1}, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;->getItemCount()I

    move-result v0

    invoke-virtual {p1}, Lio/annaclemens/xivchat/model/message/ServerInventorySummary;->getTotalQuantity()I

    move-result v1

    invoke-static {v2}, Lio/annaclemens/xivchat/ConnectionService;->access$getHandler$p(Lio/annaclemens/xivchat/ConnectionService;)Landroid/os/Handler;

    move-result-object v2

    new-instance p3, Lio/annaclemens/xivchat/InventoryUiUpdate;

    invoke-direct {p3, v3, v0, v1}, Lio/annaclemens/xivchat/InventoryUiUpdate;-><init>(Lio/annaclemens/xivchat/MainActivity;II)V

    check-cast p3, Ljava/lang/Runnable;

    invoke-virtual {v2, p3}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    goto :goto_1

    nop

    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_7
        :pswitch_0
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_3
        :pswitch_2
        :pswitch_1
        :pswitch_8
    .end packed-switch
.end method

.method final synthetic select(Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .locals 12
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lio/ktor/utils/io/ByteWriteChannel;",
            "Lcom/goterl/lazycode/lazysodium/utils/Key;",
            "Lkotlinx/coroutines/channels/ReceiveChannel<",
            "[B>;",
            "Lkotlinx/coroutines/channels/ReceiveChannel<",
            "Lkotlin/Unit;",
            ">;",
            "Lkotlin/coroutines/Continuation<",
            "-",
            "Lkotlin/Unit;",
            ">;)",
            "Ljava/lang/Object;"
        }
    .end annotation

    .line 739
    new-instance v1, Lkotlinx/coroutines/selects/SelectBuilderImpl;

    move-object/from16 v2, p5

    invoke-direct {v1, v2}, Lkotlinx/coroutines/selects/SelectBuilderImpl;-><init>(Lkotlin/coroutines/Continuation;)V

    .line 741
    :try_start_0
    move-object v0, v1

    check-cast v0, Lkotlinx/coroutines/selects/SelectBuilder;

    .line 528
    invoke-static {p0}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->access$getRunner$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;)Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    move-result-object v3

    invoke-virtual {v3}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->getDisconnectChannel()Lkotlinx/coroutines/channels/Channel;

    move-result-object v3

    invoke-interface {v3}, Lkotlinx/coroutines/channels/Channel;->getOnReceive()Lkotlinx/coroutines/selects/SelectClause1;

    move-result-object v3

    new-instance v11, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$1;

    const/4 v5, 0x0

    move-object v4, v11

    move-object v6, p0

    move-object v7, p1

    move-object v8, p2

    move-object v9, p3

    move-object/from16 v10, p4

    invoke-direct/range {v4 .. v10}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$1;-><init>(Lkotlin/coroutines/Continuation;Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;)V

    check-cast v11, Lkotlin/jvm/functions/Function2;

    invoke-interface {v0, v3, v11}, Lkotlinx/coroutines/selects/SelectBuilder;->invoke(Lkotlinx/coroutines/selects/SelectClause1;Lkotlin/jvm/functions/Function2;)V

    .line 533
    invoke-interface {p3}, Lkotlinx/coroutines/channels/ReceiveChannel;->getOnReceiveOrClosed()Lkotlinx/coroutines/selects/SelectClause1;

    move-result-object v3

    new-instance v11, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$2;

    const/4 v5, 0x0

    move-object v4, v11

    move-object v6, p0

    move-object v7, p1

    move-object v8, p2

    move-object v9, p3

    move-object/from16 v10, p4

    invoke-direct/range {v4 .. v10}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$2;-><init>(Lkotlin/coroutines/Continuation;Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;)V

    check-cast v11, Lkotlin/jvm/functions/Function2;

    invoke-interface {v0, v3, v11}, Lkotlinx/coroutines/selects/SelectBuilder;->invoke(Lkotlinx/coroutines/selects/SelectClause1;Lkotlin/jvm/functions/Function2;)V

    .line 560
    invoke-static {p0}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->access$getRunner$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;)Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    move-result-object v3

    invoke-static {v3}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->access$getService$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;)Lio/annaclemens/xivchat/ConnectionService;

    move-result-object v3

    invoke-virtual {v3}, Lio/annaclemens/xivchat/ConnectionService;->getMessages()Lkotlinx/coroutines/channels/Channel;

    move-result-object v3

    invoke-interface {v3}, Lkotlinx/coroutines/channels/Channel;->getOnReceive()Lkotlinx/coroutines/selects/SelectClause1;

    move-result-object v3

    new-instance v11, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$3;

    const/4 v5, 0x0

    move-object v4, v11

    move-object v6, p0

    move-object v7, p1

    move-object v8, p2

    move-object v9, p3

    move-object/from16 v10, p4

    invoke-direct/range {v4 .. v10}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$3;-><init>(Lkotlin/coroutines/Continuation;Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;)V

    check-cast v11, Lkotlin/jvm/functions/Function2;

    invoke-interface {v0, v3, v11}, Lkotlinx/coroutines/selects/SelectBuilder;->invoke(Lkotlinx/coroutines/selects/SelectClause1;Lkotlin/jvm/functions/Function2;)V

    .line 566
    invoke-interface/range {p4 .. p4}, Lkotlinx/coroutines/channels/ReceiveChannel;->getOnReceive()Lkotlinx/coroutines/selects/SelectClause1;

    move-result-object v3

    new-instance v11, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$4;

    const/4 v5, 0x0

    move-object v4, v11

    move-object v6, p0

    move-object v7, p1

    move-object v8, p2

    move-object v9, p3

    move-object/from16 v10, p4

    invoke-direct/range {v4 .. v10}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$4;-><init>(Lkotlin/coroutines/Continuation;Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;)V

    check-cast v11, Lkotlin/jvm/functions/Function2;

    invoke-interface {v0, v3, v11}, Lkotlinx/coroutines/selects/SelectBuilder;->invoke(Lkotlinx/coroutines/selects/SelectClause1;Lkotlin/jvm/functions/Function2;)V

    .line 571
    invoke-static {p0}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->access$getRunner$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;)Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    move-result-object v3

    invoke-virtual {v3}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->getFriendListChannel()Lkotlinx/coroutines/channels/Channel;

    move-result-object v3

    invoke-interface {v3}, Lkotlinx/coroutines/channels/Channel;->getOnReceive()Lkotlinx/coroutines/selects/SelectClause1;

    move-result-object v3

    new-instance v11, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$5;

    const/4 v5, 0x0

    move-object v4, v11

    move-object v6, p0

    move-object v7, p1

    move-object v8, p2

    move-object v9, p3

    move-object/from16 v10, p4

    invoke-direct/range {v4 .. v10}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$5;-><init>(Lkotlin/coroutines/Continuation;Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;)V

    check-cast v11, Lkotlin/jvm/functions/Function2;

    invoke-interface {v0, v3, v11}, Lkotlinx/coroutines/selects/SelectBuilder;->invoke(Lkotlinx/coroutines/selects/SelectClause1;Lkotlin/jvm/functions/Function2;)V

    .line 576
    invoke-static {p0}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;->access$getRunner$p(Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;)Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;

    move-result-object v3

    invoke-virtual {v3}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner;->getChannelChannel()Lkotlinx/coroutines/channels/Channel;

    move-result-object v3

    invoke-interface {v3}, Lkotlinx/coroutines/channels/Channel;->getOnReceive()Lkotlinx/coroutines/selects/SelectClause1;

    move-result-object v3

    new-instance v11, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$6;

    const/4 v5, 0x0

    move-object v4, v11

    move-object v6, p0

    move-object v7, p1

    move-object v8, p2

    move-object v9, p3

    move-object/from16 v10, p4

    invoke-direct/range {v4 .. v10}, Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop$select$$inlined$select$lambda$6;-><init>(Lkotlin/coroutines/Continuation;Lio/annaclemens/xivchat/ConnectionService$ConnectionRunner$EventLoop;Lio/ktor/utils/io/ByteWriteChannel;Lcom/goterl/lazycode/lazysodium/utils/Key;Lkotlinx/coroutines/channels/ReceiveChannel;Lkotlinx/coroutines/channels/ReceiveChannel;)V

    check-cast v11, Lkotlin/jvm/functions/Function2;

    invoke-interface {v0, v3, v11}, Lkotlinx/coroutines/selects/SelectBuilder;->invoke(Lkotlinx/coroutines/selects/SelectClause1;Lkotlin/jvm/functions/Function2;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception v0

    .line 743
    invoke-virtual {v1, v0}, Lkotlinx/coroutines/selects/SelectBuilderImpl;->handleBuilderException(Ljava/lang/Throwable;)V

    .line 745
    :goto_0
    invoke-virtual {v1}, Lkotlinx/coroutines/selects/SelectBuilderImpl;->getResult()Ljava/lang/Object;

    move-result-object v0

    .line 738
    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    move-result-object v1

    if-ne v0, v1, :cond_0

    invoke-static/range {p5 .. p5}, Lkotlin/coroutines/jvm/internal/DebugProbesKt;->probeCoroutineSuspended(Lkotlin/coroutines/Continuation;)V

    :cond_0
    invoke-static {}, Lkotlin/coroutines/intrinsics/IntrinsicsKt;->getCOROUTINE_SUSPENDED()Ljava/lang/Object;

    move-result-object v1

    if-ne v0, v1, :cond_1

    return-object v0

    .line 581
    :cond_1
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method

