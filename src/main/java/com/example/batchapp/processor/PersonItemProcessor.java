package com.example.batchapp.processor;

import com.example.batchapp.model.Person;
import org.springframework.batch.item.ItemProcessor;

import java.util.Random;

public class PersonItemProcessor implements ItemProcessor<Person, Person> {

    private Random random = new Random();

@Override
public Person process(final Person person) throws Exception {
    // SIMULATE CPU-BOUND WORK
    double total = 0;
    for (int i = 0; i < 5000; i++) { // A small busy-loop
        total += Math.sqrt(i);
    }
    return person;
}
}
