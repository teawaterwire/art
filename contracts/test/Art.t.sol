// SPDX-License-Identifier: UNLICENSED
pragma solidity 0.8.17;

import {Test, console2} from "forge-std/Test.sol";
import {Art} from "../src/Art.sol";
import "solmate/tokens/ERC1155.sol";

contract ArtTest is Test, ERC1155TokenReceiver {
    Art art;

    address alice = address(0xa);
    address bob = address(0xb);

    function setUp() public {
        art = new Art();
    }

    function test_addArt(string calldata name, string calldata description, string calldata image) public {
        vm.prank(alice);
        vm.expectRevert(abi.encodeWithSelector(Art.ArtDoesNotExist.selector, 0));
        art.mint(0);

        art.addArt(name, description, image);

        vm.prank(alice);
        art.mint(0);
        assertEq(art.balanceOf(alice, 0), 1);

        vm.prank(bob);
        art.mint(0);
        assertEq(art.balanceOf(bob, 0), 1);

        vm.prank(alice);
        vm.expectRevert(abi.encodeWithSelector(Art.AlreadyMinted.selector, 0));
        art.mint(0);

        assertEq(art.mintedAmount(0), 3);

        art.addArt(name, description, image);

        vm.prank(alice);
        art.mint(1);
        assertEq(art.balanceOf(alice, 1), 1);
    }
}
