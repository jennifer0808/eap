package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.ClientInfo;

import java.util.List;
import java.util.Map;

public interface ClientInfoMapper {

    int deleteByPrimaryKey(String id);

    int insert(ClientInfo record);

    int insertSelective(ClientInfo record);

    ClientInfo selectByPrimaryKey(String id);

    ClientInfo searchClientByClientCode(String clientCode);

    int updateByPrimaryKeySelective(ClientInfo record);

    int updateByPrimaryKey(ClientInfo record);

    public List<ClientInfo> searchByMap(Map record);
}