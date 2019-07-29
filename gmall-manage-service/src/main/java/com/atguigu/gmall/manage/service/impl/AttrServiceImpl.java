package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class AttrServiceImpl implements AttrService {


    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;
    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {

        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        for (PmsBaseAttrInfo baseAttrInfo:pmsBaseAttrInfos) {

            String attrid = baseAttrInfo.getId();
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            //pmsbaseAttrInfo实体类中有一个临时存放属性的list
            //就是把属性表中的数据房子PmsBaseAttrInfo的实体类中的临时变量
            pmsBaseAttrValue.setAttrId(attrid);
            List<PmsBaseAttrValue> list = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
            baseAttrInfo.setAttrValueList(list);

        }
        return pmsBaseAttrInfos;
    }

    @Override
    public void saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {

        String id = pmsBaseAttrInfo.getId();
        if (StringUtils.isBlank(id)){

            //如果ID为空的话说明为新增属性else就是修改
            //只插入有值的字段空值留空
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);

            String attrid = pmsBaseAttrInfo.getId();

            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                pmsBaseAttrValue.setAttrId(attrid);
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }
        }else {
  //修改
            PmsBaseAttrValue delAttrValue = new PmsBaseAttrValue();
            delAttrValue.setAttrId(id);
           pmsBaseAttrValueMapper.delete(delAttrValue);
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue deladdAttrValue : attrValueList) {
                       deladdAttrValue.setAttrId(id);
                   pmsBaseAttrValueMapper.insertSelective(deladdAttrValue);
            }
        }
    }
    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {

        PmsBaseAttrValue pmsBaseAttrValue=new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        List<PmsBaseAttrValue> list = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);

        return list;
    }
}
