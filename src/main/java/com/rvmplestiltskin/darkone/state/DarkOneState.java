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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent world data: current Dark One + contracts.
 * Survives restarts via SavedData in the overworld data folder.
 */
public class DarkOneState extends SavedData {

    public static final Codec<ContractOffer> OFFER_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("offerer").forGetter(ContractOffer::offerer),
                    UUIDUtil.CODEC.fieldOf("target").forGetter(ContractOffer::target),
                    Codec.STRING.fieldOf("terms").forGetter(ContractOffer::terms)
            ).apply(instance, ContractOffer::new)
    );

    public static final Codec<Contract> CONTRACT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("partyA").forGetter(Contract::partyA),
                    UUIDUtil.CODEC.fieldOf("partyB").forGetter(Contract::partyB),
                    Codec.STRING.fieldOf("terms").forGetter(Contract::terms),
                    Codec.LONG.fieldOf("createdAt").forGetter(Contract::createdAtMs)
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

    public ContractOffer peekOfferFor(UUID target) {
        for (ContractOffer o : pendingOffers) {
            if (o.target.equals(target)) return o;
        }
        return null;
    }

    public void addContract(Contract contract) {
        contracts.add(contract);
        setDirty();
    }

    public boolean removeContract(int index) {
        if (index < 0 || index >= contracts.size()) return false;
        contracts.remove(index);
        setDirty();
        return true;
    }

    public boolean removeContractsInvolving(UUID uuid) {
        boolean removed = contracts.removeIf(c -> c.partyA.equals(uuid) || c.partyB.equals(uuid));
        if (removed) setDirty();
        return removed;
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

    public static DarkOneState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return new DarkOneState();
        }
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public record ContractOffer(UUID offerer, UUID target, String terms) {}

    public record Contract(UUID partyA, UUID partyB, String terms, long createdAtMs) {}
}
