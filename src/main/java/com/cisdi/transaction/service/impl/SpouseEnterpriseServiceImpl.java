package com.cisdi.transaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cisdi.transaction.config.base.ResultCode;
import com.cisdi.transaction.config.base.ResultMsgUtil;
import com.cisdi.transaction.config.exception.BusinessException;
import com.cisdi.transaction.config.utils.AuthSqlUtil;
import com.cisdi.transaction.config.utils.CalendarUtil;
import com.cisdi.transaction.config.utils.NumberUtils;
import com.cisdi.transaction.constant.ModelConstant;
import com.cisdi.transaction.constant.SqlConstant;
import com.cisdi.transaction.constant.SystemConstant;
import com.cisdi.transaction.domain.dto.*;
import com.cisdi.transaction.domain.model.*;
import com.cisdi.transaction.domain.vo.ExportReturnMessageVO;
import com.cisdi.transaction.domain.vo.ExportReturnVO;
import com.cisdi.transaction.domain.vo.KVVO;
import com.cisdi.transaction.mapper.master.InvestInfoMapper;
import com.cisdi.transaction.mapper.master.SpouseEnterpriseMapper;
import com.cisdi.transaction.service.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @Author: tgl
 * @Description:
 * @Date: 2022/9/1 15:17
 */
@Service
public class SpouseEnterpriseServiceImpl extends ServiceImpl<SpouseEnterpriseMapper, SpouseEnterprise> implements SpouseEnterpriseService {

}