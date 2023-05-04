{}:
let
  pkgs = import (fetchTarball https://github.com/NixOS/nixpkgs/archive/619d52a37ef190017a71b26469ec434f1861c20e.tar.gz)
    { };
in
pkgs.mkShell {
  buildInputs = [
    pkgs.jdk8
    pkgs.gradle_4
    pkgs.grpcurl
  ];
  shellHook = ''
    export NIX_ENV=dev
  '';
}
