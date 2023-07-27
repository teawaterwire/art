// SPDX-License-Identifier: UNLICENSED
pragma solidity 0.8.17;

import {Script, console2} from "forge-std/Script.sol";

import {Art} from "../src/Art.sol";

contract ArtScript is Script {
    function setUp() public {}

    function run() public {
        vm.startBroadcast();

        Art art = new Art();

        vm.stopBroadcast();
    }
}
