# Lite FullNode Tool

## Introduction

Lite FullNode Tool is used to split the database of a FullNode into a `Snapshot dataset` and a `History dataset`.

- `Snapshot dataset`: the minimum dataset for quick startup of the Lite FullNode.
- `History dataset`: the archive dataset that used for historical data queries.

Remember stop the FullNode process before any operation. This tool provides the ability to specify which dataset to split.
The two datasets are split by the `latest_block_number`. Lite FullNode that startup by `Snapshot dataset` does not support query the historical data behind the `latest_block_number`,
this tool also provides a merge function that can merge `History dataset` into the database of Lite FullNode. For more API details: [HTTP&GRPC APIs](#HTTP&GRPC-APIs)

For more design details, please refer to: [TIP128](https://github.com/tronprotocol/tips/issues/128)

## Usage

### Options

This tool provides independent cutting of `Snapshot Dataset` and `History Dataset` and a merge function.

- `--operation | -o`: [ split | merge ] specifies the operation as either to split or to merge
- `--type | -t`: [ snapshot | history ] is used only with `split` to specify the type of the dataset to be split; snapshot refers to Snapshot Dataset and history refers to History Dataset.
- `--fn-data-path`: FullNode database directory
- `--dataset-path`: dataset directory, when operation is `split`, `dataset-path` is the path that store the `Snapshot Dataset` or `History Dataset`,
otherwise `dataset-path` should be the `History Dataset` path.

### Example

Start a new FullNode using the default config, then an `output-directory` will be produced in the current directory.
`output-directory` contains a sub-directory named `database` which is the database to be split.

#### Split and get a `Snapshot`

First, stop the FullNode and execute:
```
// just for simplify, locate the snapshot into `/tmp` directory,
java -jar LiteFullNodeTool.jar -o split -t snapshot --fn-data-path output-directory/database --dataset-path /tmp
```
then a `snapshot` directory will be generated in `/tmp`, pack this directory and copy it to somewhere that is ready to run a Lite Fullnode.
Do not forget rename the directory from `snapshot` to `database`.
(the default value of the storage.db.directory is `database`, make sure rename the snapshot to the specified value)

#### Split a `History`

If historical data query is needed, `History dataset` should be generated and merged into Lite FullNode.
```
// just for simplify, locate the history into `/tmp` directory,
java -jar LiteFullNodeTool.jar -o split -t history --fn-data-path output-directory/database --dataset-path /tmp
```
A `history` directory will be generated in `/tmp`, pack this directory and copy it to a Lite Fullnode.
`History dataset` always take a large storage, make sure the disk has enough volume to store the `History dataset`.

#### Merge

Both `History Dataset` and `Snapshot Dataset` have an info.properties file to identify the block height from which they are segmented.
Make sure that the `split_block_num` in `History Dataset` is not less than the corresponding value in the `Snapshot Dataset`.

After getting the `History dataset`, the Lite FullNode can merge the `History dataset` and become a real FullNode.
```
// just for simplify, assume `History dataset` is locate in /tmp
java -jar LiteFullNodeTool.jar -o merge --fn-data-path output-directory/database --dataset-path /tmp/history
```

### HTTP&GRPC APIs

Some APIs are not supported on lite fullnode, here is the list:

#### Http

| wallet/  | walletsolidity/ |
|---|---|
| getblockbyid | getblockbyid |
| getblockbylatestnum | getblockbylatestnum |
| getblockbylimitnext | getblockbylimitnext |
| getblockbynum | getblockbynum |
| getmerkletreevoucherinfo | getmerkletreevoucherinfo |
| gettransactionbyid | gettransactionbyid |
| gettransactioncountbyblocknum | gettransactioncountbyblocknum |
| gettransactioninfobyid | gettransactioninfobyid  |
| gettransactionreceiptbyid | |
| isspend | isspend |
| scanandmarknotebyivk | scanandmarknotebyivk |
| scannotebyivk | scannotebyivk |
| scannotebyovk | scannotebyovk |
| totaltransaction |  |

#### GRPC

|  protocol.Wallet | protocol.WalletSolidity  | protocol.Database  |
|---|---|---|
| GetBlockById  |  |   |
| GetBlockByLatestNum |  |   |
| GetBlockByLatestNum2  |  |   |
| GetBlockByLimitNext |  |   |
| GetBlockByLimitNext2  |  |   |
| GetBlockByNum  | GetBlockByNum | GetBlockByNum  |
| GetBlockByNum2 | GetBlockByNum2  |   |
| GetMerkleTreeVoucherInfo | GetMerkleTreeVoucherInfo  |   |
| GetTransactionById  | GetTransactionById  |   |
| GetTransactionCountByBlockNum  | GetTransactionCountByBlockNum |   |
| GetTransactionInfoById  | GetTransactionInfoById  |   |
| IsSpend  | IsSpend  |   |
| ScanAndMarkNoteByIvk | ScanAndMarkNoteByIvk  |   |
| ScanNoteByIvk | ScanNoteByIvk |   |
| ScanNoteByOvk  | ScanNoteByOvk  |   |
| TotalTransaction |   |   |

These APIs can open forcibly by set `openHistoryQueryWhenLiteFN` = true, but not recommended.