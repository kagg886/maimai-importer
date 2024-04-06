// [
//   {
//     "name": "水鱼查分器",
//     "url": "https://www.diving-fish.com/maimaidx/prober/",
//     "require": [
//       {
//         "name": "username",
//         "type": 0
//       },
//       {
//         "name": "password",
//         "type": 0
//       },
//       {
//         "name": "diff",
//         "type": 1
//       }
//     ]
//   }
// ]

export type ProtocolDescription = {
    name: string,
    url: string,
    require: Array<RequireArgs>,
    className: string
}

export type RequireArgs = {
    name: string,
    type: 0 | 1
}