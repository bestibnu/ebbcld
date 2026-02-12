alter table projects
    add column monthly_budget numeric(14, 2);

alter table projects
    add column budget_warning_threshold numeric(5, 2);
