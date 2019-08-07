package com.atguigu.gmall.manage.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import com.atguigu.gmall.util.RedisUtil;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;


    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
         //插入sku商品信息
        pmsSkuInfoMapper.insert(pmsSkuInfo);

        //获取插入的ID
        String id = pmsSkuInfo.getId();
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {

            pmsSkuAttrValue.setSkuId(id);

            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }


        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();

            for (PmsSkuImage pmsSkuImage:skuImageList){
                pmsSkuImage.setSkuId(id);
                pmsSkuImageMapper.insertSelective(pmsSkuImage);
            }

        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {

            pmsSkuSaleAttrValue.setSkuId(id);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }


    }



    @Override
    public PmsSkuInfo getSkuById(String skuId, String ip) {
        System.out.println(Thread.currentThread().getName() + ":" + ip + "开始访问" + skuId + "商品详情页面");
        Jedis jedis = null;
        PmsSkuInfo pmsSkuInfoReturn = new PmsSkuInfo();
        try {
            // 查询缓存
            jedis = redisUtil.getJedis();
            String skuJSON = jedis.get("sku:" + skuId + ":info");
            if (StringUtils.isNotBlank(skuJSON)) {
                // 查询缓存成功
                System.out.println(Thread.currentThread().getName() + ":" + ip + "缓存查询成功：" + skuId + "直接返回结果");
                pmsSkuInfoReturn = JSON.parseObject(skuJSON, PmsSkuInfo.class);
            } else {
                // 查询缓存失败，查询db
                // 查询完数据库，返回结果，同步缓存
                System.out.println(Thread.currentThread().getName() + ":" + ip + "申请分布式锁");

                String lockNo = UUID.randomUUID().toString();// 防止删除其他人的锁
                String OK = jedis.set("sku:" + skuId + ":lock", lockNo, "nx", "px", 3000);
                if (StringUtils.isNotBlank(OK) && OK.equals("OK")) {
                    System.out.println(Thread.currentThread().getName() + ":" + ip + "申请分布式锁成功");
                    pmsSkuInfoReturn = getSkuByIdFromDb(skuId);// 访问db是需要加入分布式锁的限制，限制db的访问频率
                    if (pmsSkuInfoReturn != null) {
                        System.out.println(Thread.currentThread().getName() + ":" + ip + "同步缓存，然后解锁");

                        jedis.set("sku:" + skuId + ":info", JSON.toJSONString(pmsSkuInfoReturn));
                        // 将锁解开
                        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                        Object eval = jedis.eval(script, Collections.singletonList("sku:" + skuId + ":lock"), Collections.singletonList(lockNo));
//                        String lockNo2 = jedis.get("sku:" + skuId + ":lock");// 防止删除其他人的锁
//                        if (StringUtils.isNotBlank(lockNo2) && lockNo2.equals(lockNo)) {
//                            jedis.del("sku:" + skuId + ":lock");
//                        }
                    }

                } else {
                    // 自旋
                    System.out.println(Thread.currentThread().getName() + ":" + ip + "申请分布式锁失败，开始自选，三妙后重新访问该功能。。。");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return getSkuById(skuId, ip);
                }

            }
        } finally {
            jedis.close();
        }
        return pmsSkuInfoReturn;
    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId) {
        // 缓存不存在，查询数据库
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfoReturn = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);

        if (pmsSkuInfoReturn != null) {
            pmsSkuInfoReturn.setSkuImageList(pmsSkuImages);
        }


        return pmsSkuInfoReturn;
    }

    @Override
    public List<PmsSkuInfo> getSaleAttrValuesBySpuId(String spuId) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setProductId(spuId);
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.select(pmsSkuInfo);

        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String skuId = skuInfo.getId();

            PmsSkuSaleAttrValue pmsSkuSaleAttrValue = new PmsSkuSaleAttrValue();
            pmsSkuSaleAttrValue.setSkuId(skuId);
            List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValues = pmsSkuSaleAttrValueMapper.select(pmsSkuSaleAttrValue);

            skuInfo.setSkuSaleAttrValueList(pmsSkuSaleAttrValues);
        }

        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getSearchSkuInfo() {
        List<PmsSkuInfo> skuInfoList = pmsSkuInfoMapper.selectAll();

        for (PmsSkuInfo pmsSkuInfo : skuInfoList) {

            String infoId = pmsSkuInfo.getId();

            PmsSkuImage pmsSkuImage = new PmsSkuImage();
            pmsSkuImage.setSkuId(infoId);
            List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);

            pmsSkuInfo.setSkuImageList(pmsSkuImages);

            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(infoId);
            List<PmsSkuAttrValue> skuAttrValues = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);

            pmsSkuInfo.setSkuAttrValueList(skuAttrValues);
        }

        return skuInfoList;
    }
}
