package me.aleksilassila.litematica.printer.runtime;

import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEventBus;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeComponent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeScope;
import me.aleksilassila.litematica.printer.handler.FeatureModuleSet;
import me.aleksilassila.litematica.printer.handler.InventoryAvailabilityTracker;
import me.aleksilassila.litematica.printer.handler.HudStatsManager;
import me.aleksilassila.litematica.printer.handler.scan.ScanEngine;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.RttReplayController;
import me.aleksilassila.litematica.printer.printer.PlacementRateController;
import me.aleksilassila.litematica.printer.printer.MissingMaterialTracker;
import me.aleksilassila.litematica.printer.printer.action.ActionBroker;
import me.aleksilassila.litematica.printer.utils.CooldownUtils;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.InventorySwitchGuard;
import me.aleksilassila.litematica.printer.utils.InventoryMessageCooldown;
import me.aleksilassila.litematica.printer.utils.mods.QuickShulkerBridge;
import me.aleksilassila.litematica.printer.integration.inventory.InventoryProvider;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequestCoordinator;
import me.aleksilassila.litematica.printer.integration.inventory.PlayerInventoryProvider;
import me.aleksilassila.litematica.printer.integration.inventory.ChestTrackerAdapter;
import me.aleksilassila.litematica.printer.integration.inventory.TakeItOutAdapter;
import me.aleksilassila.litematica.printer.integration.litematica.LitematicaAdapter;
import me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerAdapter;
import me.aleksilassila.litematica.printer.integration.tweakeroo.TweakerooAdapter;
import me.aleksilassila.litematica.printer.handler.handlers.bedrock.BedrockEngine;
import me.aleksilassila.litematica.printer.interaction.ToolSwitchService;
import me.aleksilassila.litematica.printer.interaction.StrippableBlockPort;
import me.aleksilassila.litematica.printer.integration.inventory.StrippableBlockAdapter;
//#if MC >= 12111
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseRestockAdapter;
//#endif
import net.minecraft.client.Minecraft;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.guide.guides.RailGuide;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the active client runtime and is the only platform tick entry point.
 * Platform callbacks and feature modules share this runtime's lifecycle.
 */
public final class PrinterRuntime {
    private final RuntimeEventBus events = new RuntimeEventBus();
    private final RuntimeScope scope = new RuntimeScope();
    private RuntimeEpoch epoch = RuntimeEpoch.INITIAL;
    private Object levelIdentity;
    private Object connectionIdentity;
    private Object disconnectedConnection;
    private long currentTick = Long.MIN_VALUE;
    private boolean resetting;
    private MaterialRequestCoordinator materialRequests;
    private final BedrockEngine bedrockEngine;
    private final FeatureModuleSet modules;
    private final ActionBroker actionBroker;
    private final ScanEngine scanEngine;
    private final InventoryAvailabilityTracker inventoryAvailability;
    private final CooldownUtils cooldownUtils;
    private final InteractionUtils interactionUtils;
    private final RttReplayController rttReplayController;
    private final PlacementRateController placementRateController;
    private final MissingMaterialTracker missingMaterials;
    private final HudStatsManager hudStats;
    private final QuickShulkerAdapter quickShulkerAdapter;
    private final ChestTrackerAdapter chestTrackerAdapter;
    private final InventorySwitchGuard inventorySwitchGuard;
    private final InventoryMessageCooldown inventoryMessageCooldown;
    private final LitematicaAdapter litematica;
    private final TweakerooAdapter tweakeroo;
    private final ToolSwitchService toolSwitchService;
    private final StrippableBlockPort strippableBlocks;
    private final me.aleksilassila.litematica.printer.integration.containersync.ContainerSyncController containerSync;

    public PrinterRuntime() {
        Minecraft client = Minecraft.getInstance();
        this.cooldownUtils = new CooldownUtils();
        this.scope.register(this.cooldownUtils);
        this.litematica = new LitematicaAdapter();
        this.bedrockEngine = new BedrockEngine(client, this.cooldownUtils, this::currentTick, this.litematica);
        this.events.subscribe(this.bedrockEngine::onRuntimeEvent);
        this.scope.register(new MinecraftInteractionRuntime(client));
        this.scope.register(this.bedrockEngine);
        this.actionBroker = new ActionBroker(this, new ActionManager());
        this.scope.register(this.actionBroker);
        this.inventorySwitchGuard = new InventorySwitchGuard(client, this::currentTick);
        this.scope.register(new InventorySwitchRuntime(this.inventorySwitchGuard));
        this.tweakeroo = new TweakerooAdapter();
        this.toolSwitchService = new ToolSwitchService(client, this.tweakeroo, this.inventorySwitchGuard);
        this.strippableBlocks = new StrippableBlockAdapter();
        this.inventoryMessageCooldown = new InventoryMessageCooldown();
        this.scope.register(this.inventoryMessageCooldown);
        this.scanEngine = new ScanEngine(this);
        this.scope.register(this.scanEngine);
        this.inventoryAvailability = new InventoryAvailabilityTracker();
        this.scope.register(this.inventoryAvailability);
        this.interactionUtils = new InteractionUtils();
        this.scope.register(this.interactionUtils);
        this.rttReplayController = new RttReplayController();
        this.scope.register(this.rttReplayController);
        this.placementRateController = new PlacementRateController(this.rttReplayController);
        this.scope.register(this.placementRateController);
        this.missingMaterials = new MissingMaterialTracker();
        this.scope.register(this.missingMaterials);
        this.hudStats = new HudStatsManager(client, this::currentTick, this.rttReplayController);
        this.scope.register(this.hudStats);
        this.quickShulkerAdapter = new QuickShulkerAdapter(this.actionBroker);
        this.scope.register(this.quickShulkerAdapter);
        this.chestTrackerAdapter = new ChestTrackerAdapter(this.actionBroker);
        this.scope.register(this.chestTrackerAdapter);
        this.containerSync = new me.aleksilassila.litematica.printer.integration.containersync.ContainerSyncController(this);
        this.scope.register(this.containerSync);
        this.modules = new FeatureModuleSet(this);
    }

    public RuntimeEpoch epoch() {
        return this.epoch;
    }

    public me.aleksilassila.litematica.printer.integration.containersync.ContainerSyncController containerSync() {
        return this.containerSync;
    }

    public long currentTick() {
        return this.currentTick;
    }

    public RuntimeEventBus events() {
        return this.events;
    }

    public BedrockEngine bedrockEngine() {
        return this.bedrockEngine;
    }

    public FeatureModuleSet modules() {
        return this.modules;
    }

    public ActionBroker actionBroker() {
        return this.actionBroker;
    }

    public ScanEngine scanEngine() {
        return this.scanEngine;
    }

    public InventoryAvailabilityTracker inventoryAvailability() {
        return this.inventoryAvailability;
    }

    public CooldownUtils cooldownUtils() {
        return this.cooldownUtils;
    }

    public InteractionUtils interactionUtils() {
        return this.interactionUtils;
    }

    public RttReplayController rttReplayController() {
        return this.rttReplayController;
    }

    public PlacementRateController placementRateController() {
        return this.placementRateController;
    }

    public MissingMaterialTracker missingMaterials() {
        return this.missingMaterials;
    }

    public HudStatsManager hudStats() {
        return this.hudStats;
    }

    public QuickShulkerAdapter quickShulkerAdapter() {
        return this.quickShulkerAdapter;
    }

    public ChestTrackerAdapter chestTrackerAdapter() {
        return this.chestTrackerAdapter;
    }

    public InventorySwitchGuard inventorySwitchGuard() {
        return this.inventorySwitchGuard;
    }

    public InventoryMessageCooldown inventoryMessageCooldown() {
        return this.inventoryMessageCooldown;
    }

    public LitematicaAdapter litematica() {
        return this.litematica;
    }

    public TweakerooAdapter tweakeroo() {
        return this.tweakeroo;
    }

    public ToolSwitchService toolSwitchService() {
        return this.toolSwitchService;
    }

    public StrippableBlockPort strippableBlocks() {
        return this.strippableBlocks;
    }

    public Minecraft client() {
        return Minecraft.getInstance();
    }

    public AutoCloseable register(RuntimeComponent component) {
        return this.scope.register(component);
    }

    public MaterialRequestCoordinator materialRequests() {
        if (this.materialRequests == null) {
            // Behavioral contract: one request walks this list once and stops at the first
            // AVAILABLE/PENDING provider. Changing the order changes user-visible pickup priority.
            List<InventoryProvider> providers = new ArrayList<>();
            providers.add(new PlayerInventoryProvider(Minecraft.getInstance()));
            providers.add(new TakeItOutAdapter(this.actionBroker));
            //#if MC >= 12111
            providers.add(new VoidWarehouseRestockAdapter(this.actionBroker));
            //#endif
            providers.add(this.quickShulkerAdapter);
            providers.add(this.chestTrackerAdapter);
            this.materialRequests = new MaterialRequestCoordinator(List.copyOf(providers));
            this.scope.register(this.materialRequests);
        }
        return this.materialRequests;
    }

    public void tick(Minecraft client) {
        Object currentLevel = client.level;
        Object currentConnection = client.getConnection();
        this.currentTick = currentLevel == null ? Long.MIN_VALUE : client.level.getGameTime();
        if (currentLevel != this.levelIdentity || currentConnection != this.connectionIdentity) {
            this.levelIdentity = currentLevel;
            this.connectionIdentity = currentConnection;
            this.reset("client_scope_changed");
        }

        this.cooldownUtils.tick();
        this.containerSync.tick();
        QuickShulkerBridge.onTick();
        this.chestTrackerAdapter.tick();
        if (this.materialRequests != null) {
            this.materialRequests.tick();
            this.quickShulkerAdapter.clearExternalRequestIfIdle(this.materialRequests.isBusy());
        }
        this.interactionUtils.preprocess();
        this.interactionUtils.onTick();
        this.modules.tick();
    }

    /** Connection mixins also run for LAN peers and on Netty threads. Resolve ownership on the client thread. */
    public void onConnectionDisconnected(Object sourceConnection) {
        Minecraft client = this.client();
        client.execute(() -> {
            var listener = client.getConnection();
            if (listener == null || listener.getConnection() != sourceConnection
                    || this.disconnectedConnection == sourceConnection) {
                return;
            }
            this.disconnectedConnection = sourceConnection;
            this.reset("disconnect");
            RailGuide.clearRepairState();
            if (Configs.Core.AUTO_DISABLE_PRINTER.getBooleanValue() && Configs.Core.WORK_SWITCH.getBooleanValue()) {
                Configs.Core.WORK_SWITCH.setBooleanValue(false);
            }
        });
    }

    public void reset(String reason) {
        if (this.resetting) {
            return;
        }
        this.resetting = true;
        try {
            RuntimeEpoch previous = this.epoch;
            this.epoch = previous.next();
            this.currentTick = Long.MIN_VALUE;
            RuntimeEvent.EpochChanged event = new RuntimeEvent.EpochChanged(previous, this.epoch, reason);
            this.scope.changeEpoch(event);
            this.events.publish(event);
        } finally {
            this.resetting = false;
        }
    }
}
