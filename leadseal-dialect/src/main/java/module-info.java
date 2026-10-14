module dev.horizon.leadseal {
  requires transitive com.fasterxml.jackson.databind;

  exports dev.horizon.leadseal;
  exports dev.horizon.leadseal.attribute;
  exports dev.horizon.leadseal.change;
  exports dev.horizon.leadseal.command;

  opens dev.horizon.leadseal to com.fasterxml.jackson.databind;
  opens dev.horizon.leadseal.attribute to com.fasterxml.jackson.databind;
  opens dev.horizon.leadseal.change to com.fasterxml.jackson.databind;

  uses dev.horizon.leadseal.Dialect;
}
