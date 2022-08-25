package com.cisdi.transaction.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.cisdi.transaction.config.base.ResultCode;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.domain.OrgTree;
import com.cisdi.transaction.domain.dto.CityDTO;
import com.cisdi.transaction.domain.dto.TestDTO;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/4 15:32
 */
@Controller
@Api(tags = "EasyPoi导入导出测试")
@RequestMapping("/easyPoi/excel")
public class EasyPoiController {
    @Autowired
    private GlobalCityInfoService globalCityInfoService;

    @Autowired
    private GbBasicInfoService gbBasicInfoService;

    @Autowired
    private PurchaseBanDealInfoSevice purchaseBanDealInfoSevice;

    @Autowired
    private GbBasicInfoThreeService threeService;

    @Autowired
    private OrgService orgService;

    @ApiOperation("从Excel导入会员列表")
    @RequestMapping(value = "/importMemberList", method = RequestMethod.POST)
    @ResponseBody
    public ResultMsgUtil<Object> importMemberList(@RequestPart("file") MultipartFile file) {
//        ImportParams params = new ImportParams();
////        params.setTitleRows(1);
//        params.setHeadRows(1);
//        try {
//            List<Member> list = ExcelImportUtil.importExcel(
//                    file.getInputStream(),
//                    TestDTO.class, params);
//            return ResultMsgUtil.success(list);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "导入失败");
//        }
        return ResultMsgUtil.failure(ResultCode.RC999.getCode(), "导入失败");
    }

    @ApiOperation("城市列表")
    @RequestMapping(value = "/cityList", method = RequestMethod.POST)
    @ResponseBody
    public ResultMsgUtil<Object> cityList(@RequestBody List<CityDTO> dtoList) {
        List<GlobalCityInfo> collect = dtoList.stream().map(t -> {
            GlobalCityInfo info = new GlobalCityInfo();
            return info.setCreateTime(new Date())
                    .setUpdateTime(new Date())
                    .setAreaCode(t.getArea_code())
                    .setCountryId(t.getCountry_id())
                    .setName(t.getName_cn());
        }).collect(Collectors.toList());
        globalCityInfoService.saveBatch(collect);
        return ResultMsgUtil.success();
    }

    @ApiOperation("城市添加")
    @RequestMapping(value = "/citySave", method = RequestMethod.POST)
    @ResponseBody
    public ResultMsgUtil<Object> citySave(@RequestBody List<String> dtoList,@RequestParam String parentId) {
//        List<GlobalCityInfo> collect = dtoList.stream().map(t -> {
//            GlobalCityInfo info = new GlobalCityInfo();
//            return info.setCreateTime(new Date())
//                    .setUpdateTime(new Date())
//                    .setName(t)
//                    .setParentId(parentId)
//                    .setAreaCode("0086");
//        }).collect(Collectors.toList());
//        globalCityInfoService.saveBatch(collect);
        return ResultMsgUtil.success();
    }


    @GetMapping("/test")
    @ResponseBody
    public ResultMsgUtil<Object> test(){
        System.out.println("进入测试");
        PurchaseBanDealInfo pu = new PurchaseBanDealInfo();
        pu.setId(UUID.randomUUID().toString());
        pu.setSupplier("测试数据");
        pu.setCode("测试数据");
        pu.setBanPurchaseCode("测试数据");
        pu.setBanPurchaseName("测试数据");
        pu.setManageCompany("测试数据");
        pu.setManageCompanyCode("测试数据");
        pu.setCreateTime(DateUtil.date());
        pu.setIsExtends("是");
        pu.setCreator("测试数据");
        pu.setCreatorAccount("测试数据");
        boolean b = false;
        try {
            System.out.println("开始测试");
             b = purchaseBanDealInfoSevice.save(pu);
            System.out.println("开始成功");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("测试失败"+e.getMessage());
            return ResultMsgUtil.failure(e.getMessage());
        }
        return ResultMsgUtil.success(b);
    }
    @GetMapping("/test2")
    @ResponseBody
    public ResultMsgUtil<Object> test2(){
        System.out.println("进入测试2");
        boolean b = false;
        try {
            System.out.println("开始测试2");
            List<GbBasicInfo> list = gbBasicInfoService.lambdaQuery().list();
            list.stream().forEach(e->{
                System.out.println(e.getName()+"----"+e.getName().replaceAll("[　*| *| *|//s*]*", ""));
            });


            System.out.println("开始成功2");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("测试失败2"+e.getMessage());
            return ResultMsgUtil.failure(e.getMessage());
        }
        return ResultMsgUtil.success(b);
    }

    @GetMapping("/test3")
    @ResponseBody
    public ResultMsgUtil<Object> test3(){
        System.out.println("进入测试3");
        boolean b = false;
        try {
            System.out.println("开始测试3");
            List<GbBasicInfoThree> gbBasicInfoThrees = gbBasicInfoService.selectOldGbBasicInfo();
            List<OldGbData> oldList = new ArrayList<>();
            if(CollectionUtil.isNotEmpty(gbBasicInfoThrees)){
                for (GbBasicInfoThree gbBasicInfoThree : gbBasicInfoThrees) {
                    OldGbData info = new OldGbData();
                    BeanUtil.copyProperties(gbBasicInfoThree, info);
                    String name = gbBasicInfoThree.getName();
                    name  = name.replaceAll("[　*| *| *|//s*]*", "");
                    info.setName(name);
                    oldList.add(info);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("测试失败3"+e.getMessage());
            return ResultMsgUtil.failure(e.getMessage());
        }
        return ResultMsgUtil.success(b);
    }
    @GetMapping("/test4")
    @ResponseBody
    public ResultMsgUtil<Object> test4(String orgCode){
         List<OrgTree> treeList = orgService.selectOrgTree(orgCode);
         return ResultMsgUtil.success(treeList);
    }
}
