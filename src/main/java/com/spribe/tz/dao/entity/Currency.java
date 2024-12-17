package com.spribe.tz.dao.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CURRENCY")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Currency {
    @Id
    private String currencyCode;
}
