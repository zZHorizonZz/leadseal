module dev.horizon.leadseal {
  requires transitive com.fasterxml.jackson.databind;

  exports dev.horizon.leadseal;

  opens dev.horizon.leadseal to com.fasterxml.jackson.databind;
  opens dev.horizon.leadseal.impl to com.fasterxml.jackson.databind;

  uses dev.horizon.leadseal.Dialect;
}
