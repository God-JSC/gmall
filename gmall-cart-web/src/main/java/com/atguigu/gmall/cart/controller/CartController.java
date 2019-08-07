package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.CookieUtil;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

  //  @Reference
  //  CartService cartService;

    @Reference
    SkuService skuService;

    @RequestMapping("addToCart")
    public String addToCart(HttpServletRequest request, HttpSession session, HttpServletResponse response, OmsCartItem omsCartItem) {

        /**
         * db中的购物车 cartListDb
         * cookie中的购物车 cartListCookie
         * 缓存中的购物车 cartListCache
         */
        String productSkuId = omsCartItem.getProductSkuId();

        PmsSkuInfo skuInfo = skuService.getSkuById(productSkuId, "");

        //设置商品是否被选中
        omsCartItem.setIsChecked("1");
        //设置商品价格
        omsCartItem.setPrice(skuInfo.getPrice());
          //设置总价格
        omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
          //设置商品默认图片
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
           //设置商品名称
        omsCartItem.setProductName(skuInfo.getSkuName());
             //设置商品平台id
        omsCartItem.setProductId(skuInfo.getProductId());
            //设置商品三级分类
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
          //设置加入购物车的时间
        omsCartItem.setCreateDate(new Date());

        Cookie[] cookies = request.getCookies();//获取副本

        Object attribute = session.getAttribute("");//获取的对象地址

        List<OmsCartItem> cartList = new ArrayList<>();
        String memberId = "";
        //判断用户是否登录
        if (StringUtils.isBlank(memberId)) {//stringUtils you isnotbank isblank
            //用户没有登录放入cookie
             String cartListCookieStr = CookieUtil.getCookieValue(request, "cartListCookie", true);

            //判断前台传来的cookie是否有数据
            if (StringUtils.isNotBlank(cartListCookieStr)) {
                //有数据
                cartList = JSON.parseArray(cartListCookieStr, OmsCartItem.class);

                boolean b=if_new_cart(cartList,omsCartItem);//判断老车里有相同商品没 
                
                if (b){
                    //新车
                    cartList.add(omsCartItem);
                }else {
                    //老车
                    for (OmsCartItem cartItem : cartList) {
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
                            break;
                        }
                    }
                }
            } else {
                //没有数据
                cartList = new ArrayList<>();

                cartList.add(omsCartItem);//前台传来的omscartItem
            }

            //覆盖Cookie
            CookieUtil.setCookie(request,response,"cartListCookie",JSON.toJSONString(cartList),60*60*24,true);


        } else {
            //已登录放入服务器的数据库
      //      cartService.addCart(new OmsCartItem());


        }


        return "redirect:/success.html";
    }

    //判断老车里有相同商品没
    private boolean if_new_cart(List<OmsCartItem> cartList, OmsCartItem omsCartItem) {
        boolean b=true;

        for (OmsCartItem cartItem:cartList){
          if( cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
              b=false;
              break;
            }

        }



        return b;
    }
}
