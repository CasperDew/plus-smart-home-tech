package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Builder
@Table(name = "scenarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Scenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hubId;
    private String name;

    @MapKeyColumn(table = "scenario_conditions", name = "sensor_id")
    @JoinTable(name = "scenario_conditions",
            joinColumns = @JoinColumn(name = "scenario_id"),
            inverseJoinColumns = @JoinColumn(name = "condition_id"))
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Map<String, Condition> conditions = new HashMap<>();

    @MapKeyColumn(table = "scenario_actions", name = "sensor_id")
    @JoinTable(name = "scenario_actions",
            joinColumns = @JoinColumn(name = "scenario_id"),
            inverseJoinColumns = @JoinColumn(name = "action_id")
    )
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Map<String, Action> actions = new HashMap<>();
}
