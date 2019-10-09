package com.leyou.item.api;


import com.leyou.item.pojo.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@RequestMapping("spec")
public interface SpecificationApi {


    @GetMapping("{id}")
    Specification querySpecificationByCategoryId(@PathVariable("id") Long id);
}
