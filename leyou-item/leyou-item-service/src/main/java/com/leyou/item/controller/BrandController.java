package com.leyou.item.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.Brand;
import com.leyou.item.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("brand")
public class BrandController {

    @Autowired
    private BrandService brandService;


    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>> queryBrandByPage(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "pages", defaultValue = "1") Integer pages,
            @RequestParam(value = "rows",  defaultValue = "5") Integer rows,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "desc", required = false) Boolean desc){
        PageResult<Brand> result =  brandService.queryBrandByPage(key,pages,rows,sortBy,desc);
        if(result == null || CollectionUtils.isEmpty(result.getItems())){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Void> saveBrand(Brand brand, @RequestParam(value = "cids")List<Long> cids){

        brandService.saveBrand(brand, cids);
        return ResponseEntity.status(HttpStatus.CREATED).build();


    }

}
