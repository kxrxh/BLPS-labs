package com.itmo.blps.lab1.bpmn;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmo.blps.lab1.dto.AdDto;
import com.itmo.blps.lab1.dto.AdvertisementResponseDto;
import com.itmo.blps.lab1.entities.RealEstateType;
import com.itmo.blps.lab1.services.core.AdvertisementService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Component
public class AdvertisementWorker {

    @Autowired
    private ExternalTaskClient externalTaskClient;

    @Autowired
    private AdvertisementService advertisementService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void subscribe() {
        externalTaskClient.subscribe("adv-create")
                .handler(this::handleCreateAdvertisement)
                .open();

        externalTaskClient.subscribe("adv-form-validation")
                .handler(this::handleValidateAdvertisement)
                .open();
    }

    private void handleCreateAdvertisement(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            String title = externalTask.getVariable("Adv_Title");
            String description = externalTask.getVariable("Adv_Desc");
            Long priceLong = externalTask.getVariable("Adv_Price");
            Double price = priceLong.doubleValue();
            String address = externalTask.getVariable("Adv_Address");
            String city = externalTask.getVariable("Adv_City");
            String realEstateTypeStr = externalTask.getVariable("Adv_Type");
            Long userId = externalTask.getVariable("userId");

            AdDto adDto = new AdDto();
            adDto.setTitle(title);
            adDto.setDescription(description);
            adDto.setPrice(price);
            adDto.setAddress(address);
            adDto.setCity(city);
            adDto.setRealEstateType(RealEstateType.valueOf(realEstateTypeStr.toUpperCase()));

            AdvertisementResponseDto response = advertisementService.createAdvertisement(adDto, userId);

            externalTaskService.complete(externalTask,
                    Map.of("advertisement", objectMapper.writeValueAsString(response)),
                    Map.of("adv_id", response.getId()));
        } catch (Exception e) {
            log.error("Error creating advertisement", e);
            externalTaskService.handleBpmnError(externalTask, "CREATE_ERROR", e.getMessage());
        }
    }

    private void handleValidateAdvertisement(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            String title = externalTask.getVariable("Adv_Title");
            String description = externalTask.getVariable("Adv_Desc");
            Long priceLong = externalTask.getVariable("Adv_Price");
            Double price = priceLong != null ? priceLong.doubleValue() : null;
            String address = externalTask.getVariable("Adv_Address");
            String city = externalTask.getVariable("Adv_City");
            String realEstateTypeStr = externalTask.getVariable("Adv_Type");
            Long userId = externalTask.getVariable("userId");

            if (title == null || description == null || price == null || address == null ||
                    city == null || realEstateTypeStr == null || userId == null) {
                throw new IllegalArgumentException("Missing required fields");
            }

            AdDto adDto = new AdDto();
            adDto.setTitle(title);
            adDto.setDescription(description);
            adDto.setPrice(price);
            adDto.setAddress(address);
            adDto.setCity(city);
            adDto.setRealEstateType(RealEstateType.valueOf(realEstateTypeStr.toUpperCase()));

            if (!adDto.isValid()) {
                throw new IllegalArgumentException("Invalid advertisement data");
            }

            externalTaskService.complete(externalTask);
        } catch (Exception e) {
            log.error("Error validating advertisement", e);
            externalTaskService.handleBpmnError(externalTask, "400", e.getMessage());
        }
    }
}
