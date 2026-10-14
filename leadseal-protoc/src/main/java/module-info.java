module dev.horizon.leadseal.protoc {
  requires dev.horizon.leadseal.protobuf;
  requires info.picocli;

  opens dev.horizon.leadseal.protoc to info.picocli;
}
