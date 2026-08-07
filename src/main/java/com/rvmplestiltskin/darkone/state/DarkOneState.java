package com.rvmplestiltskin.darkone.state;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tracks the Dark One and active contracts (in-memory for the server session).
 */
public class DarkOneState {

    private static final DarkOneState INSTANCE = new DarkOneState();

    private UUID darkOneUuid = null;

    /** Pending offers: target UUID -> offer */
    private final List<ContractOffer> pendingOffers = new ArrayList<>();
    /** Accepted contracts */
    private final List<Contract> contracts = new ArrayList<>();

    private DarkOneState() {}

    public UUID getDarkOneUuid() {
        return darkOneUuid;
    }

    public boolean isDarkOne(UUID uuid) {
        return darkOneUuid != null && darkOneUuid.equals(uuid);
    }

    public void setDarkOne(UUID uuid) {
        this.darkOneUuid = uuid;
    }

    public void clearDarkOne() {
        this.darkOneUuid = null;
    }

    public void addOffer(ContractOffer offer) {
        // Replace previous offer to same target
        pendingOffers.removeIf(o -> o.target.equals(offer.target));
        pendingOffers.add(offer);
    }

    public ContractOffer takeOfferFor(UUID target) {
        for (int i = 0; i < pendingOffers.size(); i++) {
            if (pendingOffers.get(i).target.equals(target)) {
                return pendingOffers.remove(i);
            }
        }
        return null;
    }

    public void addContract(Contract contract) {
        contracts.add(contract);
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
        return INSTANCE;
    }

    public record ContractOffer(UUID offerer, UUID target, String terms) {}

    public record Contract(UUID partyA, UUID partyB, String terms, long createdAtMs) {}
}
