package com.moving.admin.entity.talent;

import com.moving.admin.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@ApiModel("朋友介绍")
@Data
@Entity
@Table(name = "friend")
public class Friend extends BaseEntity {

    @Id
    @GeneratedValue(generator = "id_generator")
    @GenericGenerator(name = "id_generator", strategy = "identity")
    private Long id;

    private Long talentId;

    private String name;

    private String company;

    private String department;

    private String phone;

}
