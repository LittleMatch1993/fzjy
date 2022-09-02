package com.cisdi.transaction.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @Author: tgl
 * @Description:
 * @Date: 2022/8/22 23:40
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgVo {
    String id;
    String name;
    Boolean haveChildren;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgVo orgVo = (OrgVo) o;
        return Objects.equals(id, orgVo.id) &&
                Objects.equals(name, orgVo.name) &&
                Objects.equals(haveChildren, orgVo.haveChildren);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, haveChildren);
    }
}
