{
  description = "ItemLogAdmin — Paper GUI for ItemLog (Kotlin)";
  nixConfig.sandbox = "relaxed";
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachSystem [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" ] (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };
        jdk = pkgs.jdk21;
        gradle = pkgs.gradle_8;
        ideaPkg = if pkgs.jetbrains ? idea then pkgs.jetbrains.idea else pkgs.jetbrains.idea-community;
        nixFmt = pkgs.nixfmt;
        ktlint = pkgs.ktlint;
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            jdk
            gradle
            pkgs.git
            pkgs.bash
            ideaPkg
            nixFmt
            ktlint
          ];
          shellHook = ''
            export JAVA_HOME=${jdk}
            echo "ItemLogAdmin — java $(java -version 2>&1 | head -n1) | gradle $(gradle --version | grep Gradle)"
            echo "  gradle shadowJar        → build/libs/itemlogadmin-1.0.0-SNAPSHOT.jar"
            echo "  gradle spotlessCheck    → Kotlin fmt"
            echo "  nix fmt                 → Nix fmt"
            echo "  gradle idea             → generate .idea"
          '';
        };
        packages.default = pkgs.stdenv.mkDerivation {
          pname = "itemlogadmin";
          version = "1.0.0-SNAPSHOT";
          src = ./.;
          nativeBuildInputs = [
            jdk
            gradle
            pkgs.cacert
          ];
          __noChroot = true;
          buildPhase = ''
            export GRADLE_USER_HOME=$TMPDIR/.gradle
            export HOME=$TMPDIR
            gradle --no-daemon shadowJar
          '';
          installPhase = ''
            mkdir -p $out
            cp build/libs/*.jar $out/
          '';
        };
        packages.idea = ideaPkg;
        formatter = nixFmt;
      }
    );
}
