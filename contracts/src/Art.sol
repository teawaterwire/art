// SPDX-License-Identifier: UNLICENSED
pragma solidity 0.8.17;

import "solmate/tokens/ERC1155.sol";
import "solmate/auth/Owned.sol";
import "solady/utils/Base64.sol";

contract Art is ERC1155, Owned {
    uint256 public constant ART_SUPPLY = 99;

    uint256 public nextArtId;

    mapping(uint256 => uint256) public mintedAmount;

    mapping(uint256 => string) public uris;

    error SoldOut(uint256 artId);
    error AlreadyMinted(uint256 artId);
    error ArtDoesNotExist(uint256 artId);

    constructor() Owned(msg.sender) {}

    function mint(uint256 artId) public {
        if (artId >= nextArtId) revert ArtDoesNotExist(artId);
        if (mintedAmount[artId] >= ART_SUPPLY) revert SoldOut(artId);
        if (balanceOf[msg.sender][artId] > 0) revert AlreadyMinted(artId);
        ++mintedAmount[artId];
        _mint(msg.sender, artId, 1, "");
    }

    function uri(uint256 artId) public view override returns (string memory) {
        if (artId >= nextArtId) revert ArtDoesNotExist(artId);
        return uris[artId];
    }

    function addArt(string calldata name, string calldata description, string calldata image) external onlyOwner {
        string memory artURI = string(
            abi.encodePacked(
                "data:application/json;base64,",
                Base64.encode(
                    bytes(
                        abi.encodePacked(
                            '{"name":"', name, '","image":"', image, '","description":"', description, '"}'
                        )
                    )
                )
            )
        );
        uris[nextArtId] = artURI;
        mint(nextArtId++);
    }
}
