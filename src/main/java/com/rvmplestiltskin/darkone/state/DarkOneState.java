package com.rvmplestiltskin.darkone.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rvmplestiltskin.darkone.TheDarkOne;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DarkOneState extends SavedData {

    public static final Codec<ContractOffer> OFFER_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("offerer").forGetter(ContractOffer::offerer),
                    UUIDUtil.CODEC.fieldOf("target").forGetter(ContractOffer::target),
                    Codec.STRING.fieldOf("terms").forGetter(ContractOffer::terms),
                    Codec.STRING.optionalFieldOf("templateId", "").forGetter(ContractOffer::templateId)
            ).apply(instance, ContractOffer::new)
    );

    public static final Codec<Contract> CONTRACT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("partyA").forGetter(Contract::partyA),
                    UUIDUtil.CODEC.fieldOf("partyB").forGetter(Contract::partyB),
                    Codec.STRING.fieldOf("terms").forGetter(Contract::terms),
                    Codec.LONG.fieldOf("createdAt").forGetter(Contract::createdAtMs),
                    Codec.STRING.optionalFieldOf("templateId", "").forGetter(Contract::templateId),
                    Codec.INT.optionalFieldOf("violations", 0).forGetter(Contract::violations)
            ).apply(instance, Contract::new)
    );

    public static final Codec<DarkOneState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.optionalFieldOf("darkOne").forGetter(s -> Optional.ofNullable(s.darkOneUuid)),
                    OFFER_CODEC.listOf().optionalFieldOf("offers", List.of()).forGetter(s -> s.pendingOffers),
                    CONTRACT_CODEC.listOf().optionalFieldOf("contracts", List.of()).forGetter(s -> s.contracts)
            ).apply(instance, (darkOne, offers, contracts) -> {
                DarkOneState state = new DarkOneState();
                state.darkOneUuid = darkOne.orElse(null);
                state.pendingOffers = new ArrayList<>(offers);
                state.contracts = new ArrayList<>(contracts);
                return state;
            })
    );

    public static final SavedDataType<DarkOneState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TheDarkOne.MOD_ID, "state"),
            DarkOneState::new,
            CODEC,
            null
    );

    private UUID darkOneUuid = null;
    private List<ContractOffer> pendingOffers = new ArrayList<>();
    private List<Contract> contracts = new ArrayList<>();

    /** Cooldown to avoid spamming auto-punish (uuid -> last punish time ms) */
    private final Map<UUID, Long> lastAutoPunish = new HashMap<>();

    public DarkOneState() {
        super();
    }

    public UUID getDarkOneUuid() {
        return darkOneUuid;
    }

    public boolean isDarkOne(UUID uuid) {
        return darkOneUuid != null && darkOneUuid.equals(uuid);
    }

    public void setDarkOne(UUID uuid) {
        this.darkOneUuid = uuid;
        setDirty();
    }

    public void clearDarkOne() {
        this.darkOneUuid = null;
        setDirty();
    }

    public void addOffer(ContractOffer offer) {
        pendingOffers.removeIf(o -> o.target.equals(offer.target));
        pendingOffers.add(offer);
        setDirty();
    }

    public ContractOffer takeOfferFor(UUID target) {
        for (int i = 0; i < pendingOffers.size(); i++) {
            if (pendingOffers.get(i).target.equals(target)) {
                ContractOffer offer = pendingOffers.remove(i);
                setDirty();
                return offer;
            }
        }
        return null;
    }

    public void addContract(Contract contract) {
        contracts.add(contract);
        setDirty();
    }

    public List<Contract> getContracts() {
        return List.copyOf(contracts);
    }

    public List<Contract> getContractsInvolving(UUID uuid) {
        List<Contract> result = new ArrayList<>();
        for (Contract c : contracts) {
            if (c.partyA.equals(uuid) || c.partyB.equals(uuid)) {
                result.add(c);
            }
        }
        return result;
    }

    public void recordViolation(UUID offender) {
        for (int i = 0; i < contracts.size(); i++) {
            Contract c = contracts.get(i);
            if (c.partyA.equals(offender) || c.partyB.equals(offender)) {
                contracts.set(i, new Contract(c.partyA, c.partyB, c.terms, c.createdAtMs,
                        c.templateId, c.violations + 1));
            }
        }
        setDirty();
    }

    public boolean canAutoPunish(UUID uuid, long cooldownMs) {
        long now = System.currentTimeMillis();
        Long last = lastAutoPunish.get(uuid);
        if (last != null && now - last < cooldownMs) return false;
        lastAutoPunish.put(uuid, now);
        return true;
    }

    public static DarkOneState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return new DarkOneState();
        }
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public record ContractOffer(UUID offerer, UUID target, String terms, String templateId) {
        public ContractOffer(UUID offerer, UUID target, String terms) {
            this(offerer, target, terms, "");
        }
    }

    public record Contract(UUID partyA, UUID partyB, String terms, long createdAtMs,
                           String templateId, int violations) {
        public Contract(UUID partyA, UUID partyB, String terms, long createdAtMs) {
            this(partyA, partyB, terms, createdAtMs, "", 0);
        }
    }
}
