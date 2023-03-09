package com.example.discidiumbackend;

import org.hyperledger.fabric.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class HomeController {

    private final AssetService assetService;

    @Autowired
    public HomeController(AssetService assetService) {
        this.assetService = assetService;
    }


    @GetMapping("/asset/{assetId}")
    public String getAsset(@PathVariable String assetId) throws GatewayException {
        System.out.println("assetId: " + assetId);
        return assetService.readAssetById(assetId);
    }

    @GetMapping
    public String getAssets() throws GatewayException {
        return assetService.getAllAssets();
    }

    @PostMapping
    public String createAsset(@RequestBody TemperatureAssetForCatering asset) throws GatewayException, CommitException {
        return assetService.createAsset(asset);
    }

    @PutMapping("/asset/{assetId}")
    public String updateAsset(@PathVariable String assetId, @RequestBody StatusDto statusDto) throws GatewayException {
        System.out.println(statusDto.toString());
        return assetService.updateAsset(assetId, statusDto.getStatus());
    }

    @GetMapping("/init-ledger")
    public String initLedger() throws GatewayException, CommitException {
        return assetService.initLedger();
    }

    @PostMapping("/check-temperature")
    public String checkTemperature(@RequestBody TemperatureAssetForCatering asset) throws GatewayException, CommitException {
        return assetService.checkTemperature(asset);
    }
}
