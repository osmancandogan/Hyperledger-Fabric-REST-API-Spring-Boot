package com.example.discidiumbackend;
/*
 * SPDX-License-Identifier: Apache-2.0
 */


import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;
import lombok.ToString;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
@Getter
@ToString
public final class TemperatureAssetForCatering implements Serializable {

    @Property()
    private final String assetId;

    @Property()
    private final Double temperature;

    @Property()
    private final Status status;

    public TemperatureAssetForCatering(String assetID, Double temperature, Status status, String owner) {
        this.assetId = assetID;
        this.temperature = temperature;
        this.status = status;
        this.owner = owner;
    }

    @Property()
    private final String owner;


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        TemperatureAssetForCatering other = (TemperatureAssetForCatering) obj;

        return Objects.deepEquals(
                new String[]{getAssetId(), getTemperature().toString(), getOwner()},
                new String[]{other.getAssetId(), other.getTemperature().toString(), other.getOwner()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAssetId(), getTemperature(), getOwner(), getStatus());
    }
}
