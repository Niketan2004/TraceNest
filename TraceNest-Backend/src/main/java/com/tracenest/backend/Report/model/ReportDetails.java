package com.tracenest.backend.Report.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetails {
    private Integer age;
    private String gender;
    private String clothing;
    
    private String species;
    private String breed;
    private String color;
    
    private String vehicleType;
    private String vehicleNumber;
    private String manufacturer;
    private String model;
    private Integer year;
    
    private String firNumber;
    private String policeStation;
    private String policeComplaint;
}
