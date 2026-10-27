package com.plantia.controllers;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.plantia.dto.RecentScanDto;
import com.plantia.dto.ScanResultDto;
import com.plantia.services.EscaneoService;

@RestController
@RequestMapping("/api/v1/escaneos")
public class EscaneoController {

    private final EscaneoService escaneoService;

    public EscaneoController(EscaneoService escaneoService) {
        this.escaneoService = escaneoService;
    }

    /** Alimenta RecentScansRow. */
    @GetMapping("/recientes")
    public List<RecentScanDto> recientes(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId) {
        return escaneoService.recientes(userId);
    }

    /** Integracion del modulo de escaneo: sube la foto, identifica y guarda en coleccion. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ScanResultDto escanear(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Integer userId,
            @RequestPart("foto") MultipartFile foto) {
        return escaneoService.escanear(userId, foto);
    }
}
