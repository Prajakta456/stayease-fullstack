package com.codingshuttle.projects.airBnbApp.config;

import com.codingshuttle.projects.airBnbApp.dto.RoomDto;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // Fields left out of an update request keep their current value instead of being wiped
        modelMapper.getConfiguration().setSkipNullEnabled(true);

        // Stay pricing is computed per request; without this, fuzzy matching would copy basePrice into price
        modelMapper.typeMap(Room.class, RoomDto.class).addMappings(mapper -> {
            mapper.skip(RoomDto::setPrice);
            mapper.skip(RoomDto::setAvailable);
        });
        return modelMapper;
    }
}
