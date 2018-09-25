create table event(
  id serial primary key not null,
  created timestamp with time zone default current_timestamp not null,
  payload jsonb not null,
  partition_key uuid not null,
  command_id uuid not null
);

create table account(
  id uuid primary key not null,
  user_name varchar not null,
  funds decimal(13,4) not null,

  constraint uni_account_user_name unique (user_name)
);

create type reservation_state as enum('pending', 'confirmed', 'cancelled');

create table reservation(
  id uuid primary key not null,
  account_id uuid not null,
  description varchar not null,
  amount decimal(13,4) not null,
  state reservation_state not null,

  constraint fk_res_acc_id foreign key (account_id) references account (id)
);
