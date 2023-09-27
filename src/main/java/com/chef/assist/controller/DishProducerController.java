package com.chef.assist.controller;

import com.chef.assist.config.CaResponse;
import com.chef.assist.config.annotation.RoleCheck;
import com.chef.assist.constants.KitchenConstants;
import com.chef.assist.mapper.DishProducerMapper;
import com.chef.assist.mapper.OrderItemMapper;
import com.chef.assist.model.DishProducer;
import com.chef.assist.model.dto.*;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dish-producer")
public class DishProducerController {

    @Autowired
    private DishProducerMapper dishProducerMapper;

    @Autowired
    private Environment env;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @PostMapping
    public CaResponse newDishProducer(@Valid @RequestBody DishProducer dishProducer){
        try{
            dishProducer.setStatus(KitchenConstants.OFFLINE);
            dishProducerMapper.insert(dishProducer);
            return CaResponse.makeResponse(true, "成功创建厨位:"+dishProducer.getProducerNumber(),dishProducer.getProducerNumber());
        }catch (Exception e){
            if(e instanceof DuplicateKeyException){
                return CaResponse.makeResponse(false, "duplicateKitchenStationName", dishProducer.getProducerNumber());
            }
            return CaResponse.makeResponse(false, "unknownError",null);
        }
    }

    @GetMapping
    public PaginationWrapper getAllDishProducers(@RequestParam(value = "page", required = true) Integer page){
        int defaultPageSize = env.getProperty("default.page.size", Integer.class);
        PageHelper.startPage(page,defaultPageSize);
        List<DishProducer> dishProducers = dishProducerMapper.findAll();
        int totalSize = dishProducerMapper.count();

        PaginationWrapper result = new PaginationWrapper();
        result.setTotal(totalSize);
        result.setPageSize(defaultPageSize);
        result.setCurrentPage(page);
        result.setData(dishProducers);

        return result;
    }

    @GetMapping("/withitems")
    public PaginationWrapper getDishProducersWithItems(@RequestParam(value = "page", required = true) Integer page, @RequestParam(value = "online_only") Boolean onlineOnly){
        // step 1: get kits by page
        int defaultPageSize = env.getProperty("default.page.size", Integer.class);
        PageHelper.startPage(page,defaultPageSize);
        List<DishProducer> dishProducers;
        int totalSize = dishProducerMapper.count();
        if(onlineOnly==true){
            dishProducers  = dishProducerMapper.findAllNotOffline();
            totalSize = dishProducerMapper.countNotOffline();
        }else{
            dishProducers  = dishProducerMapper.findAll();
        }

        List<Long> dishProducerIds = dishProducers.stream().map(r->{return r.getId();}).collect(Collectors.toList());


        // step 2: for all dish producers got in step 1, get all assigned(active order) order items and group those items by kits
        List<KitBoardItems> kitBoardItems = new ArrayList<>();
        if(dishProducerIds==null || dishProducerIds.size()<1){

        }else{
            kitBoardItems = orderItemMapper.findAssignmentInBoards(dishProducerIds);
        }

        List<DishProducerBoardDTO> data = groupItemsByKitBoard(dishProducers, kitBoardItems);

        PaginationWrapper result = new PaginationWrapper();
        result.setTotal(totalSize);
        result.setPageSize(defaultPageSize);
        result.setCurrentPage(page);
        result.setData(data);

        return result;
    }

    private List<DishProducerBoardDTO> groupItemsByKitBoard(List<DishProducer> dishProducers, List<KitBoardItems> kitBoardItems){
        List<DishProducerBoardDTO> result = new ArrayList<>();

        Map<Long, List<DishProducerBoardDTO.BoardInnerItems>> interMediateMap = new HashMap<>();
        for(KitBoardItems k: kitBoardItems){
            if(interMediateMap.containsKey(k.getProducerId())){
                interMediateMap.get(k.getProducerId()).add(new DishProducerBoardDTO.BoardInnerItems(k.getItemId(), k.getDishName(), k.getDishCount(), k.getDescription()));
            }else{
                interMediateMap.put(k.getProducerId(), new ArrayList<>());
                interMediateMap.get(k.getProducerId()).add(new DishProducerBoardDTO.BoardInnerItems(k.getItemId(), k.getDishName(), k.getDishCount(), k.getDescription()));
            }
        }

        for(DishProducer dp: dishProducers){
            DishProducerBoardDTO dpb = new DishProducerBoardDTO();

            dpb.setDescription(dp.getDescription());
            dpb.setInnerItems(interMediateMap.get(dp.getId()));
            dpb.setProducerNumber(dp.getProducerNumber());
            dpb.setStatus(dp.getStatus());

            result.add(dpb);
        }

        return result;
    }

    @GetMapping("/to-produce/{producerId}")
    public List<ProducerViewItem> getProducerViewItemById(@PathVariable("producerId") Long producerId){
        List<ProducerViewItem> rawResult = orderItemMapper.getMyAssignedItems(producerId);
        List<ProducerViewItem> result = rawResult.stream().map(ProducerViewItem::ellipsis).collect(Collectors.toList());
        return result;
    }

    @GetMapping("/{id}")
    public DishProducer getDishProducerById(@PathVariable("id") Long id){
        return dishProducerMapper.findById(id);
    }

    @PutMapping("/{id}")
    public CaResponse editDishProducer(@PathVariable("id") Long id, @Valid @RequestBody EditDishProducerRequest request){
        DishProducer dishProducer = dishProducerMapper.findById(id);

        dishProducer.setDescription(request.getDescription());
        dishProducer.setProducerNumber(request.getProducerNumber());


        try{
            dishProducerMapper.update(dishProducer);
        }catch (Exception e){
            if(e instanceof DuplicateKeyException){
                return CaResponse.makeResponse(false, "duplicateKitchenStationName", id );
            }
            return CaResponse.makeResponse(false, "unknownErrorUpdatingKitchenStation", id );
        }

        return CaResponse.makeResponse(true, "kitchenStationUpdatedSuccessfully", id );
    }

    @DeleteMapping("/{id}")
    @RoleCheck(roles={"管理员"})
    public CaResponse deleteKit(@PathVariable("id") Long id){
        DishProducer dishProducer = dishProducerMapper.findById(id);

        if(!KitchenConstants.OFFLINE.equals(dishProducer.getStatus())){
            return CaResponse.makeResponse(false, "pleaseLogoutKitchenStationFirst", id);
        }
        try{
            dishProducerMapper.deleteById(id);
        }catch (Exception e){
            if(e instanceof DataIntegrityViolationException){
                return CaResponse.makeResponse(false,"该厨位被引用，不能删除", id);
            }
        }
        return CaResponse.makeResponse(true,"kitchenStationDeletedSuccessfully", id);
    }

    @PutMapping("/status/{id}")
    public CaResponse updateKitStatus(@PathVariable("id") Long id, @RequestBody UpdateStatusRequest req){
        DishProducer dishProducer = dishProducerMapper.findById(id);
        String currentStatus = dishProducer.getStatus();
        if(KitchenConstants.ONLINE.equals(currentStatus) && KitchenConstants.OFFLINE.equals(req.getStatus())){
            return CaResponse.makeResponse(false, "pleaseSwitchToBlockedStatusFirst", null);
        }

        if(KitchenConstants.BLOCKING.equals(currentStatus) && KitchenConstants.OFFLINE.equals(req.getStatus())){
            // if no dishes assigned to this kit
            if(orderItemMapper.countActiveUnservedByKitId(id)>0){
                return CaResponse.makeResponse(false, "pleaseFinishAllDishPreparationsBeforeLogout", null);
            }
        }

        dishProducerMapper.updateStatus(id, req.getStatus());
        return CaResponse.makeResponse(true,"kitchenStationStatusUpdatedSuccessfully", id);
    }
}
