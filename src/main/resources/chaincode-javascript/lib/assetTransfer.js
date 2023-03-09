/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

// Deterministic JSON.stringify()
const stringify = require('json-stringify-deterministic');
const sortKeysRecursive = require('sort-keys-recursive');
const { Contract } = require('fabric-contract-api');

class AssetTransfer extends Contract {

    async InitLedger(ctx) {
        const assets = [
            {
                ID: 'asset1',
                Temperature: 38,
                Owner: 'Catering',
                Status: "SOLVED",
                Timestamp: "2022"
            },
            {
                ID: 'asset2',
                Temperature: 71,
                Owner: 'Catering',
                Status: "open",
                Timestamp: "2022"
            },
            {
                ID: 'asset3',
                Temperature: 55,
                Owner: 'Catering',
                Status: "open",
                Timestamp: "2022"
            },
            {
                ID: 'asset4',
                Temperature: 52,
                Owner: 'Catering',
                Status: "open",
                Timestamp: "2022"
            },
            {
                ID: 'asset5',
                Temperature: 59,
                Owner: 'Catering',
                Status: "open",
                Timestamp: "2022"
            },
            {
                ID: 'asset6',
                Temperature: 90,
                Owner: 'Catering',
                Status: "open",
                Timestamp: "2022"
            },
        ];

        for (const asset of assets) {
            asset.docType = 'asset';
            // example of how to write to world state deterministically
            // use convetion of alphabetic order
            // we insert data in alphabetic order using 'json-stringify-deterministic' and 'sort-keys-recursive'
            // when retrieving data, in any lang, the order of data will be the same and consequently also the corresonding hash
            await ctx.stub.putState(asset.ID, Buffer.from(stringify(sortKeysRecursive(asset))));
        }
        return assets
    }

    // CreateAsset issues a new asset to the world state with given details.
    async CreateAsset(ctx, id, temperature, status, owner, timestamp) {
        const exists = await this.AssetExists(ctx, id);
        if (exists) {
            throw new Error(`The asset ${id} already exists`);
        }

        const asset = {
            ID: id,
            Temperature: temperature,
            Status: status,
            Owner: owner,
            Timestamp: timestamp
        };
        // we insert data in alphabetic order using 'json-stringify-deterministic' and 'sort-keys-recursive'
        await ctx.stub.putState(id, Buffer.from(stringify(sortKeysRecursive(asset))));
        return JSON.stringify(asset);
    }

    // ReadAsset returns the asset stored in the world state with given id.
    async ReadAsset(ctx, id) {
        const assetJSON = await ctx.stub.getState(id); // get the asset from chaincode state
        if (!assetJSON || assetJSON.length === 0) {
            throw new Error(`The asset ${id} does not exist`);
        }
        return assetJSON.toString();
    }

    // UpdateAsset updates an existing asset in the world state with provided parameters.
    async UpdateAssetStatus(ctx, id, status) {

        const exists = await this.AssetExists(ctx, id);
        if (!exists) {
            throw new Error(`The asset ${id} does not exist`);
        }
        const assetString = await this.ReadAsset(ctx, id);
        const asset = JSON.parse(assetString);
        const updatedAsset = {
            ID: id,
            Temperature: asset.Temperature,
            Status: status,
            Owner: asset.Owner,
            Timestamp: asset.Timestamp
        };
        // overwriting original asset with new asset
        console.log(updatedAsset);
        // we insert data in alphabetic order using 'json-stringify-deterministic' and 'sort-keys-recursive'
        await ctx.stub.putState(id, Buffer.from(stringify(sortKeysRecursive(updatedAsset))));
        return JSON.stringify(updatedAsset);

    }

    // DeleteAsset deletes an given asset from the world state.
    //async DeleteAsset(ctx, id) {
    //    const exists = await this.AssetExists(ctx, id);
    //    if (!exists) {
    //        throw new Error(`The asset ${id} does not exist`);
    //    }
    //    return ctx.stub.deleteState(id);
    //}

    // AssetExists returns true when asset with given ID exists in world state.
    async AssetExists(ctx, id) {
        const assetJSON = await ctx.stub.getState(id);
        return assetJSON && assetJSON.length > 0;
    }

    // TransferAsset updates the owner field of asset with given id in the world state.
    async TransferAsset(ctx, id, newOwner) {
        const assetString = await this.ReadAsset(ctx, id);
        const asset = JSON.parse(assetString);
        const oldOwner = asset.Owner;
        asset.Owner = newOwner;
        // we insert data in alphabetic order using 'json-stringify-deterministic' and 'sort-keys-recursive'
        await ctx.stub.putState(id, Buffer.from(stringify(sortKeysRecursive(asset))));
        return oldOwner;
    }

    // GetAllAssets returns all assets found in the world state.
    async GetAllAssets(ctx) {
        const allResults = [];
        // range query with empty string for startKey and endKey does an open-ended query of all assets in the chaincode namespace.
        const iterator = await ctx.stub.getStateByRange('', '');
        let result = await iterator.next();
        while (!result.done) {
            const strValue = Buffer.from(result.value.value.toString()).toString('utf8');
            let record;
            try {
                record = JSON.parse(strValue);
            } catch (err) {
                console.log(err);
                record = strValue;
            }
            allResults.push(record);
            result = await iterator.next();
        }
        return JSON.stringify(allResults);
    }

    async CheckTemperature(ctx, id, temperature, status, owner, timestamp) {
        const minimumTemperature = 60;
        const maximumTemperature = 70;
        const exists = await this.AssetExists(ctx, id);
        if (exists) {
            console.log(`The asset ${id} already exists`);
            return false;
        }
        else if (temperature < minimumTemperature || temperature > maximumTemperature) {
            console.log(`The asset ${id} has an invalid temperature`);
            const asset = this.CreateAsset(ctx, id, temperature, status, owner, timestamp);
            return asset;
        }


    }
}
module.exports = AssetTransfer;
