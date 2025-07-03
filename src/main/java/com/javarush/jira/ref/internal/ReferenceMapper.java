package com.javarush.jira.ref.internal;

import com.javarush.jira.common.BaseMapper;
import com.javarush.jira.common.TimestampMapper;
import com.javarush.jira.ref.RefTo;
import org.mapstruct.Mapper;

@Mapper(config = TimestampMapper.class, componentModel = "spring")
public interface ReferenceMapper extends BaseMapper<Reference, RefTo> {
}
