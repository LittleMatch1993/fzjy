package com.cisdi.transaction.controller;

import cn.hutool.core.date.DateUtil;
import com.cisdi.transaction.config.base.ResultCode;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.domain.dto.CityDTO;
import com.cisdi.transaction.domain.dto.TestDTO;
import com.cisdi.transaction.domain.model.GlobalCityInfo;
import com.cisdi.transaction.domain.model.PurchaseBanDealInfo;
import com.cisdi.transaction.service.GlobalCityInfoService;
import com.cisdi.transaction.service.PurchaseBanDealInfoSevice;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Member;
import java.util.Date;
import java.util.List;
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
    private PurchaseBanDealInfoSevice purchaseBanDealInfoSevice;

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
        PurchaseBanDealInfo pu = new PurchaseBanDealInfo();
        pu.setId("1111111111111");
        pu.setCreateTime(DateUtil.date());
        pu.setCode("1234567");
        pu.setIsExtends("是");
        boolean b = false;
        try {
             b = purchaseBanDealInfoSevice.save(pu);
        }catch (Exception e){
            e.printStackTrace();
            return ResultMsgUtil.failure(e.getMessage());
        }
        return ResultMsgUtil.success(b);
    }
}
